package ais.common.inventory.reservation;

import java.math.BigDecimal;

/** Hasil reservasi yang aman dibaca ulang pada retry. */
public final class InventoryReservationResult {

	public static final String APPLIED = "APPLIED";
	public static final String ALREADY_APPLIED = "ALREADY_APPLIED";
	public static final String REJECTED = "REJECTED";

	private final String status;
	private final Long reservationId;
	private final BigDecimal reservedQuantity;
	private final String message;

	public InventoryReservationResult(String status, Long reservationId,
			BigDecimal reservedQuantity, String message) {
		this.status = status == null ? "" : status.trim();
		this.reservationId = reservationId;
		this.reservedQuantity = reservedQuantity == null ? BigDecimal.ZERO : reservedQuantity;
		this.message = message == null ? "" : message.trim();
	}

	public String getStatus() { return status; }
	public Long getReservationId() { return reservationId; }
	public BigDecimal getReservedQuantity() { return reservedQuantity; }
	public String getMessage() { return message; }
	public boolean isSuccessful() { return APPLIED.equals(status) || ALREADY_APPLIED.equals(status); }
	public boolean isIdempotentReplay() { return ALREADY_APPLIED.equals(status); }
}
