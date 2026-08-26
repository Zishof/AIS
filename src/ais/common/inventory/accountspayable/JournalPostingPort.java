package ais.common.inventory.accountspayable;

import java.math.BigDecimal;
import java.util.Date;

public interface JournalPostingPort {
	boolean alreadyPosted(String sourceType, String sourceId, String eventType);
	void post(long tenantId, String sourceType, String sourceId, String eventType,
			BigDecimal amount, Date postingDate, String idempotencyKey);
}
