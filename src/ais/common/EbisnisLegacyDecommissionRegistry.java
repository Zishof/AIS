package ais.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gerbang stabilisasi dan dekomisioning artefak legacy eBisnis.
 *
 * <p>Kelas ini hanya mengevaluasi bukti. Kelas ini tidak menghentikan writer,
 * menghapus route/tabel, menjalankan DDL, atau mengubah feature flag. Tindakan
 * operasional tetap dilakukan melalui release terpisah dan runbook yang telah
 * disetujui.</p>
 */
public final class EbisnisLegacyDecommissionRegistry {

	public static final boolean DEFAULT_DECOMMISSION_ENABLED = false;

	public static final String OBSERVATION = "OBSERVATION";
	public static final String RECONCILIATION_CLOSED = "RECONCILIATION_CLOSED";
	public static final String DEPRECATED = "DEPRECATED";
	public static final String LEGACY_WRITER_STOPPED = "LEGACY_WRITER_STOPPED";
	public static final String ARCHIVED = "ARCHIVED";
	public static final String READY_FOR_REMOVAL_RELEASE = "READY_FOR_REMOVAL_RELEASE";
	public static final String COMPLETE = "COMPLETE";
	public static final String ROLLED_BACK = "ROLLED_BACK";

	public static final String ALLOWED = "ALLOWED";
	public static final String NO_CHANGE = "NO_CHANGE";
	public static final String BLOCKED_DISABLED = "BLOCKED_DISABLED";
	public static final String BLOCKED_SEQUENCE = "BLOCKED_SEQUENCE";
	public static final String BLOCKED_EVIDENCE = "BLOCKED_EVIDENCE";
	public static final String BLOCKED_DEPENDENCY = "BLOCKED_DEPENDENCY";
	public static final String BLOCKED_MONITORING = "BLOCKED_MONITORING";
	public static final String BLOCKED_SIGN_OFF = "BLOCKED_SIGN_OFF";
	public static final String ROLLBACK_REQUIRED = "ROLLBACK_REQUIRED";

	private static final Map<String, String> NEXT_STAGE =
			new LinkedHashMap<String, String>();

	static {
		NEXT_STAGE.put(OBSERVATION, RECONCILIATION_CLOSED);
		NEXT_STAGE.put(RECONCILIATION_CLOSED, DEPRECATED);
		NEXT_STAGE.put(DEPRECATED, LEGACY_WRITER_STOPPED);
		NEXT_STAGE.put(LEGACY_WRITER_STOPPED, ARCHIVED);
		NEXT_STAGE.put(ARCHIVED, READY_FOR_REMOVAL_RELEASE);
		NEXT_STAGE.put(READY_FOR_REMOVAL_RELEASE, COMPLETE);
	}

	/**
	 * Tipe implementasi bersarang {@link Scope} milik {@link EbisnisLegacyDecommissionRegistry}. Kelas ini memberi
	 * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * EbisnisLegacyDecommissionRegistry}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String moduleKey}, {@code String
	 * artifactKey}, {@code String ownerKey}, {@code String removalRelease}; operasi lokal: {@code identity}().
	 * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see EbisnisLegacyDecommissionRegistry
	 */
	public static final class Scope {
		public final String moduleKey;
		public final String artifactKey;
		public final String ownerKey;
		public final String removalRelease;

		public Scope(String moduleKey, String artifactKey, String ownerKey,
				String removalRelease) {
			this.moduleKey = wajib(moduleKey, "moduleKey");
			this.artifactKey = wajib(artifactKey, "artifactKey");
			this.ownerKey = wajib(ownerKey, "ownerKey");
			this.removalRelease = wajib(removalRelease, "removalRelease");
		}

		public String identity() {
			return moduleKey + "/" + artifactKey + "/" + removalRelease;
		}
	}

	/**
	 * Tipe implementasi bersarang {@link Policy} milik {@link EbisnisLegacyDecommissionRegistry}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * EbisnisLegacyDecommissionRegistry}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int minObservationDays}, {@code int
	 * maxErrorRateBasisPoints}, {@code int maxOpenAlertCount}. Aturan bisnis bersama tetap berada pada kelas induk
	 * atau service yang dipanggilnya.</p>
	 *
	 * @see EbisnisLegacyDecommissionRegistry
	 */
	public static final class Policy {
		public final int minObservationDays;
		public final int maxErrorRateBasisPoints;
		public final int maxOpenAlertCount;

		public Policy(int minObservationDays, int maxErrorRateBasisPoints,
				int maxOpenAlertCount) {
			if (minObservationDays < 1 || maxErrorRateBasisPoints < 0
					|| maxOpenAlertCount < 0) {
				throw new IllegalArgumentException("Policy dekomisioning tidak valid");
			}
			this.minObservationDays = minObservationDays;
			this.maxErrorRateBasisPoints = maxErrorRateBasisPoints;
			this.maxOpenAlertCount = maxOpenAlertCount;
		}
	}

	/**
	 * Tipe implementasi bersarang {@link Evidence} milik {@link EbisnisLegacyDecommissionRegistry}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * EbisnisLegacyDecommissionRegistry}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int observationDays}, {@code int
	 * errorRateBasisPoints}, {@code int openAlertCount}, {@code int reconciliationExceptionCount}, {@code int
	 * activeReaderCount}, {@code int activeWriterCount}, {@code boolean monitoringStable}, {@code boolean
	 * routesDeprecated}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see EbisnisLegacyDecommissionRegistry
	 */
	public static final class Evidence {
		public final int observationDays;
		public final int errorRateBasisPoints;
		public final int openAlertCount;
		public final int reconciliationExceptionCount;
		public final int activeReaderCount;
		public final int activeWriterCount;
		public final boolean monitoringStable;
		public final boolean routesDeprecated;
		public final boolean actionsDeprecated;
		public final boolean tablesDeprecated;
		public final boolean legacyWriterStopped;
		public final boolean mappingArchived;
		public final boolean migrationAuditArchived;
		public final boolean backupVerified;
		public final boolean restoreTestPassed;
		public final boolean replayTestPassed;
		public final boolean sopUpdated;
		public final boolean trainingCompleted;
		public final boolean runbookUpdated;
		public final boolean disasterRecoveryUpdated;
		public final boolean ownerAssigned;
		public final boolean technicalApproved;
		public final boolean businessApproved;
		public final boolean operationsApproved;
		public final boolean securityApproved;
		public final boolean separateRemovalRelease;
		public final boolean rollbackReady;
		public final boolean physicalRemovalVerified;
		public final boolean postRemovalMonitoringStable;
		public final boolean dataIntegrityIncident;

		private Evidence(Builder builder) {
			observationDays = builder.observationDays;
			errorRateBasisPoints = builder.errorRateBasisPoints;
			openAlertCount = builder.openAlertCount;
			reconciliationExceptionCount = builder.reconciliationExceptionCount;
			activeReaderCount = builder.activeReaderCount;
			activeWriterCount = builder.activeWriterCount;
			monitoringStable = builder.monitoringStable;
			routesDeprecated = builder.routesDeprecated;
			actionsDeprecated = builder.actionsDeprecated;
			tablesDeprecated = builder.tablesDeprecated;
			legacyWriterStopped = builder.legacyWriterStopped;
			mappingArchived = builder.mappingArchived;
			migrationAuditArchived = builder.migrationAuditArchived;
			backupVerified = builder.backupVerified;
			restoreTestPassed = builder.restoreTestPassed;
			replayTestPassed = builder.replayTestPassed;
			sopUpdated = builder.sopUpdated;
			trainingCompleted = builder.trainingCompleted;
			runbookUpdated = builder.runbookUpdated;
			disasterRecoveryUpdated = builder.disasterRecoveryUpdated;
			ownerAssigned = builder.ownerAssigned;
			technicalApproved = builder.technicalApproved;
			businessApproved = builder.businessApproved;
			operationsApproved = builder.operationsApproved;
			securityApproved = builder.securityApproved;
			separateRemovalRelease = builder.separateRemovalRelease;
			rollbackReady = builder.rollbackReady;
			physicalRemovalVerified = builder.physicalRemovalVerified;
			postRemovalMonitoringStable = builder.postRemovalMonitoringStable;
			dataIntegrityIncident = builder.dataIntegrityIncident;
		}
	}

	/**
	 * Tipe implementasi bersarang {@link Builder} milik {@link EbisnisLegacyDecommissionRegistry}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * EbisnisLegacyDecommissionRegistry}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int observationDays}, {@code int
	 * errorRateBasisPoints}, {@code int openAlertCount}, {@code int reconciliationExceptionCount}, {@code int
	 * activeReaderCount}, {@code int activeWriterCount}, {@code boolean monitoringStable}, {@code boolean
	 * routesDeprecated}; operasi lokal: {@code observationDays()}, {@code errorRateBasisPoints()}, {@code
	 * openAlertCount()}, {@code reconciliationExceptionCount()}, {@code activeReaderCount()}, {@code
	 * activeWriterCount()}, {@code monitoringStable()}, {@code routesDeprecated()}, {@code actionsDeprecated()},
	 * {@code tablesDeprecated}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang
	 * dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see EbisnisLegacyDecommissionRegistry
	 */
	public static final class Builder {
		private int observationDays;
		private int errorRateBasisPoints;
		private int openAlertCount;
		private int reconciliationExceptionCount;
		private int activeReaderCount;
		private int activeWriterCount;
		private boolean monitoringStable;
		private boolean routesDeprecated;
		private boolean actionsDeprecated;
		private boolean tablesDeprecated;
		private boolean legacyWriterStopped;
		private boolean mappingArchived;
		private boolean migrationAuditArchived;
		private boolean backupVerified;
		private boolean restoreTestPassed;
		private boolean replayTestPassed;
		private boolean sopUpdated;
		private boolean trainingCompleted;
		private boolean runbookUpdated;
		private boolean disasterRecoveryUpdated;
		private boolean ownerAssigned;
		private boolean technicalApproved;
		private boolean businessApproved;
		private boolean operationsApproved;
		private boolean securityApproved;
		private boolean separateRemovalRelease;
		private boolean rollbackReady;
		private boolean physicalRemovalVerified;
		private boolean postRemovalMonitoringStable;
		private boolean dataIntegrityIncident;

		public Builder observationDays(int value) { observationDays = nonNegative(value, "observationDays"); return this; }
		public Builder errorRateBasisPoints(int value) { errorRateBasisPoints = nonNegative(value, "errorRateBasisPoints"); return this; }
		public Builder openAlertCount(int value) { openAlertCount = nonNegative(value, "openAlertCount"); return this; }
		public Builder reconciliationExceptionCount(int value) { reconciliationExceptionCount = nonNegative(value, "reconciliationExceptionCount"); return this; }
		public Builder activeReaderCount(int value) { activeReaderCount = nonNegative(value, "activeReaderCount"); return this; }
		public Builder activeWriterCount(int value) { activeWriterCount = nonNegative(value, "activeWriterCount"); return this; }
		public Builder monitoringStable(boolean value) { monitoringStable = value; return this; }
		public Builder routesDeprecated(boolean value) { routesDeprecated = value; return this; }
		public Builder actionsDeprecated(boolean value) { actionsDeprecated = value; return this; }
		public Builder tablesDeprecated(boolean value) { tablesDeprecated = value; return this; }
		public Builder legacyWriterStopped(boolean value) { legacyWriterStopped = value; return this; }
		public Builder mappingArchived(boolean value) { mappingArchived = value; return this; }
		public Builder migrationAuditArchived(boolean value) { migrationAuditArchived = value; return this; }
		public Builder backupVerified(boolean value) { backupVerified = value; return this; }
		public Builder restoreTestPassed(boolean value) { restoreTestPassed = value; return this; }
		public Builder replayTestPassed(boolean value) { replayTestPassed = value; return this; }
		public Builder sopUpdated(boolean value) { sopUpdated = value; return this; }
		public Builder trainingCompleted(boolean value) { trainingCompleted = value; return this; }
		public Builder runbookUpdated(boolean value) { runbookUpdated = value; return this; }
		public Builder disasterRecoveryUpdated(boolean value) { disasterRecoveryUpdated = value; return this; }
		public Builder ownerAssigned(boolean value) { ownerAssigned = value; return this; }
		public Builder technicalApproved(boolean value) { technicalApproved = value; return this; }
		public Builder businessApproved(boolean value) { businessApproved = value; return this; }
		public Builder operationsApproved(boolean value) { operationsApproved = value; return this; }
		public Builder securityApproved(boolean value) { securityApproved = value; return this; }
		public Builder separateRemovalRelease(boolean value) { separateRemovalRelease = value; return this; }
		public Builder rollbackReady(boolean value) { rollbackReady = value; return this; }
		public Builder physicalRemovalVerified(boolean value) { physicalRemovalVerified = value; return this; }
		public Builder postRemovalMonitoringStable(boolean value) { postRemovalMonitoringStable = value; return this; }
		public Builder dataIntegrityIncident(boolean value) { dataIntegrityIncident = value; return this; }

		public Evidence build() { return new Evidence(this); }
	}

	/**
	 * Tipe implementasi bersarang {@link Decision} milik {@link EbisnisLegacyDecommissionRegistry}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * EbisnisLegacyDecommissionRegistry}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code boolean allowed}, {@code boolean
	 * rollbackRequired}, {@code String code}, {@code String message}, {@code String scopeIdentity}, {@code List
	 * missingEvidence}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see EbisnisLegacyDecommissionRegistry
	 */
	public static final class Decision {
		public final boolean allowed;
		public final boolean rollbackRequired;
		public final String code;
		public final String message;
		public final String scopeIdentity;
		public final List<String> missingEvidence;

		private Decision(boolean allowed, boolean rollbackRequired, String code,
				String message, Scope scope, List<String> missingEvidence) {
			this.allowed = allowed;
			this.rollbackRequired = rollbackRequired;
			this.code = code;
			this.message = message;
			this.scopeIdentity = scope.identity();
			this.missingEvidence = Collections.unmodifiableList(
					new ArrayList<String>(missingEvidence));
		}
	}

	private EbisnisLegacyDecommissionRegistry() {
	}

	public static Policy conservativePolicy() {
		return new Policy(30, 25, 0);
	}

	public static List<String> orderedStages() {
		List<String> result = new ArrayList<String>();
		result.add(OBSERVATION);
		result.add(RECONCILIATION_CLOSED);
		result.add(DEPRECATED);
		result.add(LEGACY_WRITER_STOPPED);
		result.add(ARCHIVED);
		result.add(READY_FOR_REMOVAL_RELEASE);
		result.add(COMPLETE);
		return Collections.unmodifiableList(result);
	}

	public static Decision evaluate(Scope scope, String currentStage,
			String requestedStage, boolean enabled, Policy policy,
			Evidence evidence) {
		if (scope == null || policy == null || evidence == null) {
			throw new IllegalArgumentException("Scope, policy, dan evidence wajib diisi");
		}
		validateStage(currentStage);
		validateStage(requestedStage);
		if (!enabled) {
			return blocked(scope, BLOCKED_DISABLED,
					"Dekomisioning nonaktif; tidak ada artefak legacy yang boleh diubah");
		}
		if (evidence.dataIntegrityIncident) {
			return decision(false, true, ROLLBACK_REQUIRED,
					"Insiden integritas data mewajibkan rollback", scope,
					Collections.<String>emptyList());
		}
		if (currentStage.equals(requestedStage)) {
			return decision(true, false, NO_CHANGE, "Tahap tidak berubah", scope,
					Collections.<String>emptyList());
		}
		if (ROLLED_BACK.equals(requestedStage)) {
			if (!evidence.rollbackReady) {
				return missing(scope, BLOCKED_EVIDENCE,
						"Rollback belum siap", "rollbackReady");
			}
			return decision(true, true, ALLOWED, "Rollback diizinkan", scope,
					Collections.<String>emptyList());
		}
		String expected = NEXT_STAGE.get(currentStage);
		if (expected == null || !expected.equals(requestedStage)) {
			return blocked(scope, BLOCKED_SEQUENCE,
					"Tahap harus berurutan; tahap berikutnya: " + expected);
		}

		List<String> missing = missingFor(requestedStage, policy, evidence);
		if (!missing.isEmpty()) {
			String code = classifyMissing(missing);
			return decision(false, false, code,
					"Bukti dekomisioning belum lengkap", scope, missing);
		}
		return decision(true, false, ALLOWED,
				"Gerbang tahap " + requestedStage + " terpenuhi", scope,
				Collections.<String>emptyList());
	}

	private static List<String> missingFor(String stage, Policy policy,
			Evidence evidence) {
		List<String> missing = new ArrayList<String>();
		if (RECONCILIATION_CLOSED.equals(stage)) {
			if (evidence.observationDays < policy.minObservationDays) missing.add("observationDays");
			monitoring(policy, evidence, missing);
			if (evidence.reconciliationExceptionCount != 0) missing.add("reconciliationExceptionCount");
		} else if (DEPRECATED.equals(stage)) {
			if (!evidence.routesDeprecated) missing.add("routesDeprecated");
			if (!evidence.actionsDeprecated) missing.add("actionsDeprecated");
			if (!evidence.tablesDeprecated) missing.add("tablesDeprecated");
		} else if (LEGACY_WRITER_STOPPED.equals(stage)) {
			if (!evidence.legacyWriterStopped) missing.add("legacyWriterStopped");
			if (evidence.activeWriterCount != 0) missing.add("activeWriterCount");
			if (!evidence.rollbackReady) missing.add("rollbackReady");
		} else if (ARCHIVED.equals(stage)) {
			if (!evidence.mappingArchived) missing.add("mappingArchived");
			if (!evidence.migrationAuditArchived) missing.add("migrationAuditArchived");
		} else if (READY_FOR_REMOVAL_RELEASE.equals(stage)) {
			if (evidence.activeReaderCount != 0) missing.add("activeReaderCount");
			if (evidence.activeWriterCount != 0) missing.add("activeWriterCount");
			monitoring(policy, evidence, missing);
			if (!evidence.backupVerified) missing.add("backupVerified");
			if (!evidence.restoreTestPassed) missing.add("restoreTestPassed");
			if (!evidence.replayTestPassed) missing.add("replayTestPassed");
			if (!evidence.sopUpdated) missing.add("sopUpdated");
			if (!evidence.trainingCompleted) missing.add("trainingCompleted");
			if (!evidence.runbookUpdated) missing.add("runbookUpdated");
			if (!evidence.disasterRecoveryUpdated) missing.add("disasterRecoveryUpdated");
			if (!evidence.ownerAssigned) missing.add("ownerAssigned");
			if (!evidence.separateRemovalRelease) missing.add("separateRemovalRelease");
			if (!evidence.technicalApproved) missing.add("technicalApproved");
			if (!evidence.businessApproved) missing.add("businessApproved");
			if (!evidence.operationsApproved) missing.add("operationsApproved");
			if (!evidence.securityApproved) missing.add("securityApproved");
		} else if (COMPLETE.equals(stage)) {
			if (!evidence.physicalRemovalVerified) missing.add("physicalRemovalVerified");
			if (!evidence.postRemovalMonitoringStable) missing.add("postRemovalMonitoringStable");
			monitoring(policy, evidence, missing);
		}
		return missing;
	}

	private static void monitoring(Policy policy, Evidence evidence,
			List<String> missing) {
		if (!evidence.monitoringStable) missing.add("monitoringStable");
		if (evidence.errorRateBasisPoints > policy.maxErrorRateBasisPoints) missing.add("errorRateBasisPoints");
		if (evidence.openAlertCount > policy.maxOpenAlertCount) missing.add("openAlertCount");
	}

	private static String classifyMissing(List<String> missing) {
		if (containsAny(missing, "activeReaderCount", "activeWriterCount")) return BLOCKED_DEPENDENCY;
		if (containsAny(missing, "monitoringStable", "errorRateBasisPoints", "openAlertCount")) return BLOCKED_MONITORING;
		if (containsAny(missing, "technicalApproved", "businessApproved", "operationsApproved", "securityApproved")) return BLOCKED_SIGN_OFF;
		return BLOCKED_EVIDENCE;
	}

	private static boolean containsAny(List<String> values, String a, String b) {
		return values.contains(a) || values.contains(b);
	}

	private static boolean containsAny(List<String> values, String a, String b,
			String c) {
		return values.contains(a) || values.contains(b) || values.contains(c);
	}

	private static boolean containsAny(List<String> values, String a, String b,
			String c, String d) {
		return values.contains(a) || values.contains(b) || values.contains(c)
				|| values.contains(d);
	}

	private static Decision blocked(Scope scope, String code, String message) {
		return decision(false, false, code, message, scope,
				Collections.<String>emptyList());
	}

	private static Decision missing(Scope scope, String code, String message,
			String evidence) {
		List<String> missing = new ArrayList<String>();
		missing.add(evidence);
		return decision(false, false, code, message, scope, missing);
	}

	private static Decision decision(boolean allowed, boolean rollbackRequired,
			String code, String message, Scope scope, List<String> missing) {
		return new Decision(allowed, rollbackRequired, code, message, scope, missing);
	}

	public static void validate() {
		List<String> stages = orderedStages();
		if (stages.size() != 7 || NEXT_STAGE.size() != 6) {
			throw new IllegalStateException("Registry tahap dekomisioning tidak lengkap");
		}
		for (int i = 0; i < stages.size() - 1; i++) {
			if (!stages.get(i + 1).equals(NEXT_STAGE.get(stages.get(i)))) {
				throw new IllegalStateException("Urutan tahap dekomisioning rusak");
			}
		}
	}

	private static void validateStage(String stage) {
		if (!orderedStages().contains(stage) && !ROLLED_BACK.equals(stage)) {
			throw new IllegalArgumentException("Tahap dekomisioning tidak dikenal: " + stage);
		}
	}

	private static String wajib(String value, String field) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(field + " wajib diisi");
		}
		return value.trim();
	}

	private static int nonNegative(int value, String field) {
		if (value < 0) throw new IllegalArgumentException(field + " tidak boleh negatif");
		return value;
	}
}
