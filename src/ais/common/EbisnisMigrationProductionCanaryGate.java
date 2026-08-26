package ais.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Gerbang canary produksi per tenant/lokasi dengan default OFF. */
public final class EbisnisMigrationProductionCanaryGate {

	public static final class Evidence {
		public final long checkedRecords;
		public final long mismatchCount;
		public final boolean backupRestorePassed;
		public final boolean crashRecoveryPassed;
		public final boolean rollbackRehearsed;
		public final boolean qaApproved;
		public final boolean businessApproved;
		public final boolean itApproved;

		public Evidence(long checkedRecords, long mismatchCount,
				boolean backupRestorePassed, boolean crashRecoveryPassed,
				boolean rollbackRehearsed, boolean qaApproved,
				boolean businessApproved, boolean itApproved) {
			this.checkedRecords = checkedRecords;
			this.mismatchCount = mismatchCount;
			this.backupRestorePassed = backupRestorePassed;
			this.crashRecoveryPassed = crashRecoveryPassed;
			this.rollbackRehearsed = rollbackRehearsed;
			this.qaApproved = qaApproved;
			this.businessApproved = businessApproved;
			this.itApproved = itApproved;
		}
	}

	public static final class Decision {
		public final boolean allowed;
		public final List<String> reasons;

		private Decision(boolean allowed, List<String> reasons) {
			this.allowed = allowed;
			this.reasons = Collections.unmodifiableList(reasons);
		}
	}

	private EbisnisMigrationProductionCanaryGate() {
	}

	public static Decision evaluate(String tenantKey, String locationKey,
			EbisnisMigrationOperationalControl.FeatureFlag featureFlag,
			Evidence evidence) {
		if (featureFlag == null || evidence == null) {
			throw new IllegalArgumentException("featureFlag dan evidence wajib ada");
		}
		String scope = required(tenantKey, "tenantKey") + "|"
				+ required(locationKey, "locationKey");
		List<String> reasons = new ArrayList<String>();
		boolean enabled = false;
		try {
			enabled = featureFlag.isEnabled(scope, "MIGRATION", "CANARY");
		} catch (RuntimeException ignored) {
			enabled = false;
		}
		if (!enabled) reasons.add("Feature flag canary tenant/lokasi masih nonaktif");
		if (evidence.checkedRecords <= 0) reasons.add("Belum ada record yang diperiksa");
		if (evidence.mismatchCount != 0) reasons.add("Rekonsiliasi masih memiliki mismatch");
		if (!evidence.backupRestorePassed) reasons.add("Drill backup-restore belum lulus");
		if (!evidence.crashRecoveryPassed) reasons.add("Drill crash recovery belum lulus");
		if (!evidence.rollbackRehearsed) reasons.add("Rollback rehearsal belum lulus");
		if (!evidence.qaApproved) reasons.add("Persetujuan QA belum ada");
		if (!evidence.businessApproved) reasons.add("Persetujuan bisnis belum ada");
		if (!evidence.itApproved) reasons.add("Persetujuan IT belum ada");
		return new Decision(reasons.isEmpty(), reasons);
	}

	private static String required(String value, String name) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(name + " wajib diisi");
		}
		return value.trim();
	}
}
