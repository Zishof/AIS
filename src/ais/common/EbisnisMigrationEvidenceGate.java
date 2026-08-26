package ais.common;

import java.io.IOException;
import java.util.List;

/**
 * Gerbang fail-closed untuk tindakan rollout/dekomisioning berisiko tinggi.
 * GuardedAction wajib idempoten terhadap operationId.
 */
public final class EbisnisMigrationEvidenceGate {

	public static final String RESULT_APPLIED = "APPLIED";
	public static final String RESULT_ALREADY_APPLIED = "ALREADY_APPLIED";

	public interface ActorAuthenticator {
		boolean isAuthorized(String actor, String workflow, String stage);
	}

	public interface GuardedAction {
		void execute(String operationId) throws Exception;
	}

	public static final class Plan {
		public final String operationId;
		public final String workflow;
		public final String scopeIdentity;
		public final String stage;
		public final String actor;
		public final String reference;
		public final String evidencePayload;

		public Plan(String operationId, String workflow, String scopeIdentity,
				String stage, String actor, String reference,
				String evidencePayload) {
			this.operationId = required(operationId, "operationId");
			this.workflow = required(workflow, "workflow");
			this.scopeIdentity = required(scopeIdentity, "scopeIdentity");
			this.stage = required(stage, "stage");
			this.actor = required(actor, "actor");
			this.reference = required(reference, "reference");
			this.evidencePayload = required(evidencePayload, "evidencePayload");
		}
	}

	public static final class Result {
		public final String status;
		public final long preparedSequence;
		public final long appliedSequence;

		private Result(String status, long preparedSequence,
				long appliedSequence) {
			this.status = status;
			this.preparedSequence = preparedSequence;
			this.appliedSequence = appliedSequence;
		}
	}

	public static final class Metrics {
		public final long attempts;
		public final long applied;
		public final long alreadyApplied;
		public final long rejected;
		public final long evidenceFailures;
		public final long actionFailures;

		private Metrics(long attempts, long applied, long alreadyApplied,
				long rejected, long evidenceFailures, long actionFailures) {
			this.attempts = attempts;
			this.applied = applied;
			this.alreadyApplied = alreadyApplied;
			this.rejected = rejected;
			this.evidenceFailures = evidenceFailures;
			this.actionFailures = actionFailures;
		}
	}

	public static final class GateException extends Exception {
		private static final long serialVersionUID = 1L;
		public final String code;

		private GateException(String code, String message, Throwable cause) {
			super(message, cause);
			this.code = code;
		}
	}

	private final EbisnisMigrationEvidenceRepository repository;
	private final ActorAuthenticator authenticator;
	private long attempts;
	private long applied;
	private long alreadyApplied;
	private long rejected;
	private long evidenceFailures;
	private long actionFailures;

	public EbisnisMigrationEvidenceGate(
			EbisnisMigrationEvidenceRepository repository,
			ActorAuthenticator authenticator) {
		if (repository == null) throw new IllegalArgumentException("repository wajib diisi");
		if (authenticator == null) throw new IllegalArgumentException("authenticator wajib diisi");
		this.repository = repository;
		this.authenticator = authenticator;
	}

	public synchronized Result execute(Plan plan, GuardedAction action)
			throws GateException {
		if (plan == null) throw new IllegalArgumentException("plan wajib diisi");
		if (action == null) throw new IllegalArgumentException("action wajib diisi");
		attempts++;
		if (!authenticator.isAuthorized(plan.actor, plan.workflow, plan.stage)) {
			rejected++;
			throw new GateException("ACTOR_NOT_AUTHORIZED",
					"Actor tidak diizinkan menjalankan tahap migrasi", null);
		}

		List<EbisnisMigrationEvidenceJournal.Entry> entries = checkedEntries();
		EbisnisMigrationEvidenceJournal.Entry existingApplied = find(entries,
				plan.operationId + ":APPLIED");
		if (existingApplied != null) {
			alreadyApplied++;
			return new Result(RESULT_ALREADY_APPLIED, 0L,
					existingApplied.sequence);
		}

		EbisnisMigrationEvidenceJournal.Entry prepared = find(entries,
				plan.operationId + ":PREPARED");
		if (prepared == null) {
			prepared = append(plan, ":PREPARED", "PREPARED", "AUTHORIZED",
					plan.evidencePayload);
		}
		checkedEntries();

		try {
			action.execute(plan.operationId);
		} catch (Exception e) {
			actionFailures++;
			appendFailureOnce(plan, e);
			throw new GateException("GUARDED_ACTION_FAILED",
					"Tindakan migrasi gagal; status APPLIED tidak diterbitkan", e);
		}

		EbisnisMigrationEvidenceJournal.Entry appliedEntry = append(plan,
				":APPLIED", "APPLIED", "COMPLETED", plan.evidencePayload);
		checkedEntries();
		applied++;
		return new Result(RESULT_APPLIED, prepared.sequence,
				appliedEntry.sequence);
	}

	public synchronized Metrics metrics() {
		return new Metrics(attempts, applied, alreadyApplied, rejected,
				evidenceFailures, actionFailures);
	}

	private List<EbisnisMigrationEvidenceJournal.Entry> checkedEntries()
			throws GateException {
		try {
			EbisnisMigrationEvidenceJournal.Verification verification =
					repository.verify();
			if (!verification.valid) {
				evidenceFailures++;
				throw new GateException("EVIDENCE_CORRUPT",
						"Integritas evidence gagal: " + verification.message, null);
			}
			return repository.read();
		} catch (IOException e) {
			evidenceFailures++;
			throw new GateException("EVIDENCE_UNAVAILABLE",
					"Repository evidence tidak tersedia", e);
		}
	}

	private EbisnisMigrationEvidenceJournal.Entry append(Plan plan,
			String eventSuffix, String stage, String decisionCode,
			String payload) throws GateException {
		try {
			return repository.append(new EbisnisMigrationEvidenceJournal.Request(
					System.currentTimeMillis(), plan.operationId + eventSuffix,
					plan.workflow, plan.scopeIdentity, stage, decisionCode,
					plan.actor, plan.reference, payload));
		} catch (IOException e) {
			evidenceFailures++;
			throw new GateException("EVIDENCE_APPEND_FAILED",
					"Evidence tidak dapat disimpan; tindakan ditolak", e);
		} catch (RuntimeException e) {
			evidenceFailures++;
			throw new GateException("EVIDENCE_CONFLICT",
					"Evidence konflik; tindakan ditolak", e);
		}
	}

	private void appendFailureOnce(Plan plan, Exception failure)
			throws GateException {
		List<EbisnisMigrationEvidenceJournal.Entry> entries = checkedEntries();
		if (find(entries, plan.operationId + ":FAILED") != null) return;
		String reason = failure.getClass().getName();
		append(plan, ":FAILED", "FAILED", "ACTION_FAILED", reason);
		checkedEntries();
	}

	private static EbisnisMigrationEvidenceJournal.Entry find(
			List<EbisnisMigrationEvidenceJournal.Entry> entries,
			String eventId) {
		for (int i = 0; i < entries.size(); i++) {
			EbisnisMigrationEvidenceJournal.Entry entry = entries.get(i);
			if (eventId.equals(entry.eventId)) return entry;
		}
		return null;
	}

	private static String required(String value, String name) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(name + " wajib diisi");
		}
		return value;
	}
}
