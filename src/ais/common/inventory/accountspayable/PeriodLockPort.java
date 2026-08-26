package ais.common.inventory.accountspayable;

import java.util.Date;

public interface PeriodLockPort {
	boolean isLocked(long tenantId, Date postingDate);
}
