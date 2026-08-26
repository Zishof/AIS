package ais.common.inventory.control;

import java.math.BigDecimal;
import java.util.Date;

/** Kandidat lot untuk alokasi First Expired First Out. */
public final class FefoLotCandidate {
	private final Long lotId;
	private final BigDecimal availableQuantity;
	private final Date expiryAt;
	private final boolean quarantined;

	public FefoLotCandidate(Long lotId, BigDecimal availableQuantity, Date expiryAt,
			boolean quarantined) {
		this.lotId = lotId;
		this.availableQuantity = availableQuantity;
		this.expiryAt = expiryAt == null ? null : new Date(expiryAt.getTime());
		this.quarantined = quarantined;
	}

	public Long getLotId() { return lotId; }
	public BigDecimal getAvailableQuantity() { return availableQuantity; }
	public Date getExpiryAt() { return expiryAt == null ? null : new Date(expiryAt.getTime()); }
	public boolean isQuarantined() { return quarantined; }
}
