package ais.common;

/**
 * Kontrol operasional F17 untuk menghubungkan feature flag dan identitas nyata
 * ke evidence gate. Semua keputusan gagal secara tertutup.
 */
public final class EbisnisMigrationOperationalControl {

	public interface FeatureFlag {
		boolean isEnabled(String scopeIdentity, String workflow, String stage);
	}

	public interface IdentityProvider {
		boolean authenticateAndAuthorize(String actor, String credential,
				String workflow, String stage);
	}

	public static final FeatureFlag DISABLED = new FeatureFlag() {
		public boolean isEnabled(String scopeIdentity, String workflow,
				String stage) {
			return false;
		}
	};

	private final EbisnisMigrationEvidenceRepository repository;
	private final FeatureFlag featureFlag;
	private final IdentityProvider identityProvider;

	public EbisnisMigrationOperationalControl(
			EbisnisMigrationEvidenceRepository repository,
			FeatureFlag featureFlag, IdentityProvider identityProvider) {
		if (repository == null) throw new IllegalArgumentException("repository wajib diisi");
		if (featureFlag == null) throw new IllegalArgumentException("featureFlag wajib diisi");
		if (identityProvider == null) throw new IllegalArgumentException("identityProvider wajib diisi");
		this.repository = repository;
		this.featureFlag = featureFlag;
		this.identityProvider = identityProvider;
	}

	public synchronized EbisnisMigrationEvidenceGate.Result execute(
			final EbisnisMigrationEvidenceGate.Plan plan,
			final String credential,
			EbisnisMigrationEvidenceGate.GuardedAction action)
			throws EbisnisMigrationEvidenceGate.GateException {
		if (plan == null) throw new IllegalArgumentException("plan wajib diisi");
		if (credential == null || credential.trim().length() == 0) {
			throw new IllegalArgumentException("credential wajib diisi");
		}
		if (!featureFlag.isEnabled(plan.scopeIdentity, plan.workflow,
				plan.stage)) {
			throw disabled();
		}
		if (!identityProvider.authenticateAndAuthorize(plan.actor, credential,
				plan.workflow, plan.stage)) {
			throw unauthorized();
		}
		EbisnisMigrationEvidenceGate.ActorAuthenticator authenticatedActor =
				new EbisnisMigrationEvidenceGate.ActorAuthenticator() {
			public boolean isAuthorized(String actor, String workflow,
					String stage) {
				return plan.actor.equals(actor)
						&& plan.workflow.equals(workflow)
						&& plan.stage.equals(stage);
			}
		};
		return new EbisnisMigrationEvidenceGate(repository,
				authenticatedActor).execute(plan, action);
	}

	private static EbisnisMigrationEvidenceGate.GateException disabled() {
		return new EbisnisMigrationEvidenceGate.GateException(
				"ROLLOUT_DISABLED", "Feature flag tahap migrasi belum aktif",
				null);
	}

	private static EbisnisMigrationEvidenceGate.GateException unauthorized() {
		return new EbisnisMigrationEvidenceGate.GateException(
				"IDENTITY_NOT_AUTHORIZED",
				"Identitas tidak terautentikasi atau tidak berwenang", null);
	}
}
