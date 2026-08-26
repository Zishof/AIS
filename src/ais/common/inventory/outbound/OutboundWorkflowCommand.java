package ais.common.inventory.outbound;

import java.util.Date;

/** Perubahan status/custody yang persisten dan idempoten. */
public final class OutboundWorkflowCommand {
	private final String aggregateType;
	private final String aggregateId;
	private final String action;
	private final String idempotencyKey;
	private final Date occurredAt;
	private final String note;

	public OutboundWorkflowCommand(String aggregateType, String aggregateId,
			String action, String idempotencyKey, Date occurredAt, String note) {
		this.aggregateType = clean(aggregateType);
		this.aggregateId = clean(aggregateId);
		this.action = clean(action);
		this.idempotencyKey = clean(idempotencyKey);
		this.occurredAt = copy(occurredAt);
		this.note = clean(note);
		if (this.aggregateType.length() == 0 || this.aggregateId.length() == 0
				|| this.action.length() == 0 || this.idempotencyKey.length() == 0 || occurredAt == null) {
			throw new IllegalArgumentException("Perintah workflow outbound tidak lengkap");
		}
	}

	public String getAggregateType() { return aggregateType; }
	public String getAggregateId() { return aggregateId; }
	public String getAction() { return action; }
	public String getIdempotencyKey() { return idempotencyKey; }
	public Date getOccurredAt() { return copy(occurredAt); }
	public String getNote() { return note; }

	private static String clean(String value) { return value == null ? "" : value.trim(); }
	private static Date copy(Date value) { return value == null ? null : new Date(value.getTime()); }
}
