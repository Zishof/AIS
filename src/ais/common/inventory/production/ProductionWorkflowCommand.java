package ais.common.inventory.production;

import java.util.Date;

/** Perintah perubahan workflow produksi yang wajib idempoten pada adapter persistence. */
public final class ProductionWorkflowCommand {
	private final Long tenantId;
	private final Long locationId;
	private final String orderId;
	private final String action;
	private final String referenceId;
	private final String idempotencyKey;
	private final Date businessAt;

	public ProductionWorkflowCommand(Long tenantId, Long locationId, String orderId,
			String action, String referenceId, String idempotencyKey, Date businessAt) {
		this.tenantId = tenantId;
		this.locationId = locationId;
		this.orderId = clean(orderId);
		this.action = clean(action);
		this.referenceId = clean(referenceId);
		this.idempotencyKey = clean(idempotencyKey);
		this.businessAt = copy(businessAt);
		if (tenantId == null || locationId == null || this.orderId.length() == 0
				|| this.action.length() == 0 || this.referenceId.length() == 0
				|| this.idempotencyKey.length() == 0 || businessAt == null) {
			throw new IllegalArgumentException("Perintah workflow produksi tidak lengkap");
		}
	}

	public Long getTenantId() { return tenantId; }
	public Long getLocationId() { return locationId; }
	public String getOrderId() { return orderId; }
	public String getAction() { return action; }
	public String getReferenceId() { return referenceId; }
	public String getIdempotencyKey() { return idempotencyKey; }
	public Date getBusinessAt() { return copy(businessAt); }

	private static String clean(String value) { return value == null ? "" : value.trim(); }
	private static Date copy(Date value) { return value == null ? null : new Date(value.getTime()); }
}
