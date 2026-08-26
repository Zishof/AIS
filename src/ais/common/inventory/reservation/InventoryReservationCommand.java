package ais.common.inventory.reservation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import ais.common.inventory.ledger.InventoryBalanceKey;

/** Perintah reserve/release/consume stok dengan kunci retry tersendiri. */
public final class InventoryReservationCommand {

	public static final String RESERVE = "RESERVE";
	public static final String RELEASE = "RELEASE";
	public static final String CONSUME = "CONSUME";

	private final String action;
	private final String reservationKey;
	private final String idempotencyKey;
	private final InventoryBalanceKey balanceKey;
	private final BigDecimal quantity;
	private final Date expiresAt;

	public InventoryReservationCommand(String action, String reservationKey,
			String idempotencyKey, InventoryBalanceKey balanceKey,
			BigDecimal quantity, Date expiresAt) {
		this.action = bersihkan(action);
		this.reservationKey = bersihkan(reservationKey);
		this.idempotencyKey = bersihkan(idempotencyKey);
		this.balanceKey = balanceKey;
		this.quantity = quantity;
		this.expiresAt = salin(expiresAt);
	}

	public List<String> validate() {
		List<String> errors = new ArrayList<String>();
		if (!RESERVE.equals(action) && !RELEASE.equals(action) && !CONSUME.equals(action)) {
			errors.add("action reservasi tidak dikenal");
		}
		if (reservationKey.length() == 0) errors.add("reservationKey wajib diisi");
		if (idempotencyKey.length() == 0) errors.add("idempotencyKey wajib diisi");
		if (balanceKey == null) errors.add("balanceKey wajib diisi");
		if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
			errors.add("quantity reservasi harus lebih besar dari nol");
		}
		if (RESERVE.equals(action) && expiresAt == null) errors.add("expiresAt wajib untuk reserve");
		return Collections.unmodifiableList(errors);
	}

	public boolean isValid() { return validate().isEmpty(); }
	public String getAction() { return action; }
	public String getReservationKey() { return reservationKey; }
	public String getIdempotencyKey() { return idempotencyKey; }
	public InventoryBalanceKey getBalanceKey() { return balanceKey; }
	public BigDecimal getQuantity() { return quantity; }
	public Date getExpiresAt() { return salin(expiresAt); }

	private static String bersihkan(String value) {
		return value == null ? "" : value.trim();
	}

	private static Date salin(Date value) {
		return value == null ? null : new Date(value.getTime());
	}
}
