package ais.common.inventory.accountspayable;

/** Status kanonis tagihan vendor pada subledger hutang usaha. */
public final class AccountsPayableStatus {
	public static final String DRAFT = "DRAFT";
	public static final String MATCH_EXCEPTION = "MATCH_EXCEPTION";
	public static final String MATCHED = "MATCHED";
	public static final String APPROVED = "APPROVED";
	public static final String PARTIALLY_PAID = "PARTIALLY_PAID";
	public static final String PAID = "PAID";
	public static final String CANCELLED = "CANCELLED";
	private AccountsPayableStatus() { }
}
