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

	/**
	 * Kontrak callback/strategi bersarang milik {@link EbisnisMigrationEvidenceGate}. Tipe ini memisahkan satu
	 * variasi perilaku lokal tanpa membuat service atau interface global yang tumpang tindih.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link EbisnisMigrationEvidenceGate} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code isAuthorized}(). Aturan bisnis
	 * bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see EbisnisMigrationEvidenceGate
	 */
	public interface ActorAuthenticator {
		boolean isAuthorized(String actor, String workflow, String stage);
	}

	/**
	 * Kontrak callback/strategi bersarang milik {@link EbisnisMigrationEvidenceGate}. Tipe ini memisahkan satu
	 * variasi perilaku lokal tanpa membuat service atau interface global yang tumpang tindih.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link EbisnisMigrationEvidenceGate} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code execute}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see EbisnisMigrationEvidenceGate
	 */
	public interface GuardedAction {
		void execute(String operationId) throws Exception;
	}

	/**
	 * Tipe implementasi bersarang {@link Plan} milik {@link EbisnisMigrationEvidenceGate}. Kelas ini memberi nama
	 * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * EbisnisMigrationEvidenceGate}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String operationId}, {@code String
	 * workflow}, {@code String scopeIdentity}, {@code String stage}, {@code String actor}, {@code String
	 * reference}, {@code String evidencePayload}. Aturan bisnis bersama tetap berada pada kelas induk atau service
	 * yang dipanggilnya.</p>
	 *
	 * @see EbisnisMigrationEvidenceGate
	 */
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

	/**
	 * Pembawa data/helper lokal milik {@link EbisnisMigrationEvidenceGate} untuk result. Tipe ini mengelompokkan
	 * nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * EbisnisMigrationEvidenceGate}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String status}, {@code long
	 * preparedSequence}, {@code long appliedSequence}. Aturan bisnis bersama tetap berada pada kelas induk atau
	 * service yang dipanggilnya.</p>
	 *
	 * @see EbisnisMigrationEvidenceGate
	 */
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

	/**
	 * Tipe implementasi bersarang {@link Metrics} milik {@link EbisnisMigrationEvidenceGate}. Kelas ini memberi
	 * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * EbisnisMigrationEvidenceGate}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code long attempts}, {@code long applied},
	 * {@code long alreadyApplied}, {@code long rejected}, {@code long evidenceFailures}, {@code long
	 * actionFailures}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see EbisnisMigrationEvidenceGate
	 */
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

	/**
	 * Tipe implementasi bersarang {@link GateException} milik {@link EbisnisMigrationEvidenceGate}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * EbisnisMigrationEvidenceGate}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String code}. Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see EbisnisMigrationEvidenceGate
	 */
	public static final class GateException extends Exception {
		private static final long serialVersionUID = 1L;
		public final String code;

		GateException(String code, String message, Throwable cause) {
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
