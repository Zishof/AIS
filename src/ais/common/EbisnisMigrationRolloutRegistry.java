package ais.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gerbang migrasi dan rollout bertahap eBisnis.
 *
 * <p>Kelas ini hanya mengevaluasi bukti dan urutan rollout. Kelas ini tidak
 * menjalankan DDL, backfill, mengubah feature flag, atau memindahkan writer.
 * Aktivasi produksi tetap harus dilakukan oleh operator melalui runbook dan
 * persetujuan yang terdokumentasi.</p>
 */
public final class EbisnisMigrationRolloutRegistry {

	public static final boolean DEFAULT_ROLLOUT_ENABLED = false;

	public static final String BASELINE = "BASELINE";
	public static final String DRY_RUN = "DRY_RUN";
	public static final String BACKFILL = "BACKFILL";
	public static final String SHADOW_READ = "SHADOW_READ";
	public static final String SHADOW_WRITE = "SHADOW_WRITE";
	public static final String RECONCILIATION = "RECONCILIATION";
	public static final String CANARY = "CANARY";
	public static final String CUTOVER = "CUTOVER";
	public static final String COMPLETE = "COMPLETE";
	public static final String ROLLED_BACK = "ROLLED_BACK";

	public static final String ALLOWED = "ALLOWED";
	public static final String NO_CHANGE = "NO_CHANGE";
	public static final String BLOCKED_DISABLED = "BLOCKED_DISABLED";
	public static final String BLOCKED_SEQUENCE = "BLOCKED_SEQUENCE";
	public static final String BLOCKED_EVIDENCE = "BLOCKED_EVIDENCE";
	public static final String BLOCKED_HEALTH = "BLOCKED_HEALTH";
	public static final String ROLLBACK_REQUIRED = "ROLLBACK_REQUIRED";

	private static final Map<String, String> NEXT_STAGE =
			new LinkedHashMap<String, String>();

	static {
		NEXT_STAGE.put(BASELINE, DRY_RUN);
		NEXT_STAGE.put(DRY_RUN, BACKFILL);
		NEXT_STAGE.put(BACKFILL, SHADOW_READ);
		NEXT_STAGE.put(SHADOW_READ, SHADOW_WRITE);
		NEXT_STAGE.put(SHADOW_WRITE, RECONCILIATION);
		NEXT_STAGE.put(RECONCILIATION, CANARY);
		NEXT_STAGE.put(CANARY, CUTOVER);
		NEXT_STAGE.put(CUTOVER, COMPLETE);
	}

	/**
	 * Tipe implementasi bersarang {@link Scope} milik {@link EbisnisMigrationRolloutRegistry}. Kelas ini memberi
	 * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * EbisnisMigrationRolloutRegistry}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String tenantKey}, {@code String
	 * locationKey}, {@code String writerKey}, {@code int canaryPercent}; operasi lokal: {@code identity}(). Aturan
	 * bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see EbisnisMigrationRolloutRegistry
	 */
	public static final class Scope {
		public final String tenantKey;
		public final String locationKey;
		public final String writerKey;
		public final int canaryPercent;

		public Scope(String tenantKey, String locationKey, String writerKey,
				int canaryPercent) {
			this.tenantKey = wajib(tenantKey, "tenantKey");
			this.locationKey = wajib(locationKey, "locationKey");
			this.writerKey = wajib(writerKey, "writerKey");
			if (canaryPercent < 0 || canaryPercent > 100) {
				throw new IllegalArgumentException(
						"canaryPercent harus antara 0 dan 100");
			}
			this.canaryPercent = canaryPercent;
		}

		public String identity() {
			return tenantKey + "/" + locationKey + "/" + writerKey;
		}
	}

	/**
	 * Tipe implementasi bersarang {@link Policy} milik {@link EbisnisMigrationRolloutRegistry}. Kelas ini memberi
	 * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * EbisnisMigrationRolloutRegistry}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code long maxMismatchCount}, {@code int
	 * maxErrorRateBasisPoints}, {@code int maxLatencyRegressionPercent}, {@code int minObservationMinutes}, {@code
	 * int maxCanaryPercent}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
	 * dipanggilnya.</p>
	 *
	 * @see EbisnisMigrationRolloutRegistry
	 */
	public static final class Policy {
		public final long maxMismatchCount;
		public final int maxErrorRateBasisPoints;
		public final int maxLatencyRegressionPercent;
		public final int minObservationMinutes;
		public final int maxCanaryPercent;

		public Policy(long maxMismatchCount, int maxErrorRateBasisPoints,
				int maxLatencyRegressionPercent, int minObservationMinutes,
				int maxCanaryPercent) {
			if (maxMismatchCount < 0 || maxErrorRateBasisPoints < 0
					|| maxLatencyRegressionPercent < 0
					|| minObservationMinutes < 1 || maxCanaryPercent < 1
					|| maxCanaryPercent > 100) {
				throw new IllegalArgumentException("Policy rollout tidak valid");
			}
			this.maxMismatchCount = maxMismatchCount;
			this.maxErrorRateBasisPoints = maxErrorRateBasisPoints;
			this.maxLatencyRegressionPercent = maxLatencyRegressionPercent;
			this.minObservationMinutes = minObservationMinutes;
			this.maxCanaryPercent = maxCanaryPercent;
		}
	}

	/**
	 * Tipe implementasi bersarang {@link Evidence} milik {@link EbisnisMigrationRolloutRegistry}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * EbisnisMigrationRolloutRegistry}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code boolean baselineCaptured}, {@code
	 * boolean dryRunPassed}, {@code boolean backfillPassed}, {@code boolean shadowReadPassed}, {@code boolean
	 * shadowWritePassed}, {@code boolean reconciliationPassed}, {@code boolean canaryPassed}, {@code boolean
	 * cutoverStable}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see EbisnisMigrationRolloutRegistry
	 */
	public static final class Evidence {
		public final boolean baselineCaptured;
		public final boolean dryRunPassed;
		public final boolean backfillPassed;
		public final boolean shadowReadPassed;
		public final boolean shadowWritePassed;
		public final boolean reconciliationPassed;
		public final boolean canaryPassed;
		public final boolean cutoverStable;
		public final boolean rollbackRehearsed;
		public final boolean qaApproved;
		public final boolean businessApproved;
		public final boolean financeApproved;
		public final boolean warehouseApproved;
		public final boolean itApproved;
		public final boolean dataIntegrityIncident;
		public final long checkedRecords;
		public final long mismatchCount;
		public final int errorRateBasisPoints;
		public final int latencyRegressionPercent;
		public final int observationMinutes;

		private Evidence(Builder builder) {
			baselineCaptured = builder.baselineCaptured;
			dryRunPassed = builder.dryRunPassed;
			backfillPassed = builder.backfillPassed;
			shadowReadPassed = builder.shadowReadPassed;
			shadowWritePassed = builder.shadowWritePassed;
			reconciliationPassed = builder.reconciliationPassed;
			canaryPassed = builder.canaryPassed;
			cutoverStable = builder.cutoverStable;
			rollbackRehearsed = builder.rollbackRehearsed;
			qaApproved = builder.qaApproved;
			businessApproved = builder.businessApproved;
			financeApproved = builder.financeApproved;
			warehouseApproved = builder.warehouseApproved;
			itApproved = builder.itApproved;
			dataIntegrityIncident = builder.dataIntegrityIncident;
			checkedRecords = builder.checkedRecords;
			mismatchCount = builder.mismatchCount;
			errorRateBasisPoints = builder.errorRateBasisPoints;
			latencyRegressionPercent = builder.latencyRegressionPercent;
			observationMinutes = builder.observationMinutes;
		}
	}

	/**
	 * Tipe implementasi bersarang {@link Builder} milik {@link EbisnisMigrationRolloutRegistry}. Kelas ini memberi
	 * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * EbisnisMigrationRolloutRegistry}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code boolean baselineCaptured}, {@code
	 * boolean dryRunPassed}, {@code boolean backfillPassed}, {@code boolean shadowReadPassed}, {@code boolean
	 * shadowWritePassed}, {@code boolean reconciliationPassed}, {@code boolean canaryPassed}, {@code boolean
	 * cutoverStable}; operasi lokal: {@code baselineCaptured()}, {@code dryRunPassed()}, {@code backfillPassed()},
	 * {@code shadowReadPassed()}, {@code shadowWritePassed()}, {@code reconciliationPassed()}, {@code
	 * canaryPassed()}, {@code cutoverStable()}, {@code rollbackRehearsed()}, {@code qaApproved}(). Aturan bisnis
	 * bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see EbisnisMigrationRolloutRegistry
	 */
	public static final class Builder {
		private boolean baselineCaptured;
		private boolean dryRunPassed;
		private boolean backfillPassed;
		private boolean shadowReadPassed;
		private boolean shadowWritePassed;
		private boolean reconciliationPassed;
		private boolean canaryPassed;
		private boolean cutoverStable;
		private boolean rollbackRehearsed;
		private boolean qaApproved;
		private boolean businessApproved;
		private boolean financeApproved;
		private boolean warehouseApproved;
		private boolean itApproved;
		private boolean dataIntegrityIncident;
		private long checkedRecords;
		private long mismatchCount;
		private int errorRateBasisPoints;
		private int latencyRegressionPercent;
		private int observationMinutes;

		public Builder baselineCaptured(boolean value) { baselineCaptured = value; return this; }
		public Builder dryRunPassed(boolean value) { dryRunPassed = value; return this; }
		public Builder backfillPassed(boolean value) { backfillPassed = value; return this; }
		public Builder shadowReadPassed(boolean value) { shadowReadPassed = value; return this; }
		public Builder shadowWritePassed(boolean value) { shadowWritePassed = value; return this; }
		public Builder reconciliationPassed(boolean value) { reconciliationPassed = value; return this; }
		public Builder canaryPassed(boolean value) { canaryPassed = value; return this; }
		public Builder cutoverStable(boolean value) { cutoverStable = value; return this; }
		public Builder rollbackRehearsed(boolean value) { rollbackRehearsed = value; return this; }
		public Builder qaApproved(boolean value) { qaApproved = value; return this; }
		public Builder businessApproved(boolean value) { businessApproved = value; return this; }
		public Builder financeApproved(boolean value) { financeApproved = value; return this; }
		public Builder warehouseApproved(boolean value) { warehouseApproved = value; return this; }
		public Builder itApproved(boolean value) { itApproved = value; return this; }
		public Builder dataIntegrityIncident(boolean value) { dataIntegrityIncident = value; return this; }
		public Builder checkedRecords(long value) { checkedRecords = value; return this; }
		public Builder mismatchCount(long value) { mismatchCount = value; return this; }
		public Builder errorRateBasisPoints(int value) { errorRateBasisPoints = value; return this; }
		public Builder latencyRegressionPercent(int value) { latencyRegressionPercent = value; return this; }
		public Builder observationMinutes(int value) { observationMinutes = value; return this; }

		public Evidence build() {
			if (checkedRecords < 0 || mismatchCount < 0
					|| errorRateBasisPoints < 0
					|| latencyRegressionPercent < 0
					|| observationMinutes < 0) {
				throw new IllegalArgumentException("Evidence rollout tidak valid");
			}
			return new Evidence(this);
		}
	}

	/**
	 * Tipe implementasi bersarang {@link Decision} milik {@link EbisnisMigrationRolloutRegistry}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * EbisnisMigrationRolloutRegistry}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code boolean allowed}, {@code boolean
	 * rollbackRequired}, {@code String code}, {@code String currentStage}, {@code String requestedStage}, {@code
	 * String scopeIdentity}, {@code List reasons}. Aturan bisnis bersama tetap berada pada kelas induk atau
	 * service yang dipanggilnya.</p>
	 *
	 * @see EbisnisMigrationRolloutRegistry
	 */
	public static final class Decision {
		public final boolean allowed;
		public final boolean rollbackRequired;
		public final String code;
		public final String currentStage;
		public final String requestedStage;
		public final String scopeIdentity;
		public final List<String> reasons;

		private Decision(boolean allowed, boolean rollbackRequired, String code,
				String currentStage, String requestedStage, String scopeIdentity,
				List<String> reasons) {
			this.allowed = allowed;
			this.rollbackRequired = rollbackRequired;
			this.code = code;
			this.currentStage = currentStage;
			this.requestedStage = requestedStage;
			this.scopeIdentity = scopeIdentity;
			this.reasons = Collections.unmodifiableList(
					new ArrayList<String>(reasons));
		}
	}

	private EbisnisMigrationRolloutRegistry() {
	}

	public static Policy conservativePolicy() {
		return new Policy(0L, 50, 15, 1440, 10);
	}

	public static Decision evaluate(Scope scope, String currentStage,
			String requestedStage, boolean rolloutEnabled, Policy policy,
			Evidence evidence) {
		if (scope == null || policy == null || evidence == null) {
			throw new IllegalArgumentException("Scope, policy, dan evidence wajib ada");
		}
		String current = stage(currentStage);
		String requested = stage(requestedStage);
		List<String> reasons = new ArrayList<String>();

		if (current.equals(requested)) {
			return decision(true, false, NO_CHANGE, current, requested, scope,
					reasons);
		}
		if (ROLLED_BACK.equals(requested)) {
			if (BASELINE.equals(current) || ROLLED_BACK.equals(current)) {
				reasons.add("Tidak ada rollout aktif yang perlu di-rollback");
				return decision(false, false, BLOCKED_SEQUENCE, current,
						requested, scope, reasons);
			}
			if (!evidence.rollbackRehearsed) {
				reasons.add("Rollback rehearsal belum lulus");
				return decision(false, true, BLOCKED_EVIDENCE, current,
						requested, scope, reasons);
			}
			return decision(true, false, ALLOWED, current, requested, scope,
					reasons);
		}
		if (!rolloutEnabled) {
			reasons.add("Feature flag rollout masih nonaktif");
			return decision(false, false, BLOCKED_DISABLED, current, requested,
					scope, reasons);
		}
		if (!requested.equals(NEXT_STAGE.get(current))) {
			reasons.add("Tahap rollout tidak boleh dilompati");
			return decision(false, false, BLOCKED_SEQUENCE, current, requested,
					scope, reasons);
		}

		boolean healthProblem = appendHealthProblems(policy, evidence, reasons);
		if (healthProblem && (SHADOW_WRITE.equals(requested)
				|| RECONCILIATION.equals(requested) || CANARY.equals(requested)
				|| CUTOVER.equals(requested) || COMPLETE.equals(requested))) {
			return decision(false, true, ROLLBACK_REQUIRED, current, requested,
					scope, reasons);
		}

		appendEvidenceProblems(scope, requested, policy, evidence, reasons);
		if (!reasons.isEmpty()) {
			return decision(false, false, BLOCKED_EVIDENCE, current, requested,
					scope, reasons);
		}
		return decision(true, false, ALLOWED, current, requested, scope,
				reasons);
	}

	public static List<String> orderedStages() {
		List<String> stages = new ArrayList<String>();
		stages.add(BASELINE);
		stages.add(DRY_RUN);
		stages.add(BACKFILL);
		stages.add(SHADOW_READ);
		stages.add(SHADOW_WRITE);
		stages.add(RECONCILIATION);
		stages.add(CANARY);
		stages.add(CUTOVER);
		stages.add(COMPLETE);
		return Collections.unmodifiableList(stages);
	}

	public static void validate() {
		List<String> stages = orderedStages();
		for (int i = 0; i < stages.size() - 1; i++) {
			String next = NEXT_STAGE.get(stages.get(i));
			if (!stages.get(i + 1).equals(next)) {
				throw new IllegalStateException("Urutan rollout tidak konsisten");
			}
		}
		if (NEXT_STAGE.containsKey(COMPLETE)
				|| NEXT_STAGE.containsKey(ROLLED_BACK)) {
			throw new IllegalStateException("Tahap terminal tidak boleh punya next");
		}
	}

	private static void appendEvidenceProblems(Scope scope, String requested,
			Policy policy, Evidence evidence, List<String> reasons) {
		if (DRY_RUN.equals(requested)) {
			require(evidence.baselineCaptured, "Baseline belum direkam", reasons);
			require(evidence.rollbackRehearsed,
					"Rollback rehearsal belum lulus", reasons);
		} else if (BACKFILL.equals(requested)) {
			require(evidence.dryRunPassed, "Dry run belum lulus", reasons);
		} else if (SHADOW_READ.equals(requested)) {
			require(evidence.backfillPassed, "Backfill belum lulus", reasons);
		} else if (SHADOW_WRITE.equals(requested)) {
			require(evidence.shadowReadPassed,
					"Shadow read belum lulus", reasons);
		} else if (RECONCILIATION.equals(requested)) {
			require(evidence.shadowWritePassed,
					"Shadow write belum lulus", reasons);
			require(evidence.checkedRecords > 0,
					"Belum ada record yang direkonsiliasi", reasons);
		} else if (CANARY.equals(requested)) {
			require(evidence.reconciliationPassed,
					"Rekonsiliasi belum lulus", reasons);
			require(evidence.rollbackRehearsed,
					"Rollback rehearsal belum lulus", reasons);
			require(evidence.qaApproved, "Persetujuan QA belum ada", reasons);
			require(evidence.itApproved, "Persetujuan IT belum ada", reasons);
			if (scope.canaryPercent < 1
					|| scope.canaryPercent > policy.maxCanaryPercent) {
				reasons.add("Persentase canary di luar batas policy");
			}
		} else if (CUTOVER.equals(requested)) {
			require(evidence.canaryPassed, "Canary belum lulus", reasons);
			require(evidence.observationMinutes >= policy.minObservationMinutes,
					"Waktu observasi belum memenuhi policy", reasons);
			requireAllApprovals(evidence, reasons);
		} else if (COMPLETE.equals(requested)) {
			require(evidence.cutoverStable,
					"Cutover belum dinyatakan stabil", reasons);
			require(evidence.observationMinutes >= policy.minObservationMinutes,
					"Waktu observasi belum memenuhi policy", reasons);
			requireAllApprovals(evidence, reasons);
		}
	}

	private static boolean appendHealthProblems(Policy policy,
			Evidence evidence, List<String> reasons) {
		boolean problem = false;
		if (evidence.dataIntegrityIncident) {
			reasons.add("Terdapat insiden integritas data");
			problem = true;
		}
		if (evidence.mismatchCount > policy.maxMismatchCount) {
			reasons.add("Jumlah mismatch melewati batas policy");
			problem = true;
		}
		if (evidence.errorRateBasisPoints > policy.maxErrorRateBasisPoints) {
			reasons.add("Error rate melewati batas policy");
			problem = true;
		}
		if (evidence.latencyRegressionPercent
				> policy.maxLatencyRegressionPercent) {
			reasons.add("Regresi latensi melewati batas policy");
			problem = true;
		}
		return problem;
	}

	private static void requireAllApprovals(Evidence evidence,
			List<String> reasons) {
		require(evidence.qaApproved, "Persetujuan QA belum ada", reasons);
		require(evidence.businessApproved,
				"Persetujuan business owner belum ada", reasons);
		require(evidence.financeApproved,
				"Persetujuan Finance belum ada", reasons);
		require(evidence.warehouseApproved,
				"Persetujuan Warehouse belum ada", reasons);
		require(evidence.itApproved, "Persetujuan IT belum ada", reasons);
	}

	private static void require(boolean condition, String reason,
			List<String> reasons) {
		if (!condition) {
			reasons.add(reason);
		}
	}

	private static Decision decision(boolean allowed, boolean rollbackRequired,
			String code, String current, String requested, Scope scope,
			List<String> reasons) {
		return new Decision(allowed, rollbackRequired, code, current, requested,
				scope.identity(), reasons);
	}

	private static String stage(String value) {
		String normalized = value == null ? "" : value.trim().toUpperCase();
		if (!orderedStages().contains(normalized)
				&& !ROLLED_BACK.equals(normalized)) {
			throw new IllegalArgumentException("Tahap rollout tidak dikenal: "
					+ value);
		}
		return normalized;
	}

	private static String wajib(String value, String field) {
		String normalized = value == null ? "" : value.trim();
		if (normalized.length() == 0) {
			throw new IllegalArgumentException(field + " wajib diisi");
		}
		return normalized;
	}
}
