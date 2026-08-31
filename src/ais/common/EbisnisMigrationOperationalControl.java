package ais.common;

/**
 * Kontrol operasional F17 untuk menghubungkan feature flag dan identitas nyata
 * ke evidence gate. Semua keputusan gagal secara tertutup.
 */
public final class EbisnisMigrationOperationalControl {

	/**
	 * Kontrak callback/strategi bersarang milik {@link EbisnisMigrationOperationalControl}. Tipe ini memisahkan
	 * satu variasi perilaku lokal tanpa membuat service atau interface global yang tumpang tindih.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link EbisnisMigrationOperationalControl} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code isEnabled}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see EbisnisMigrationOperationalControl
	 */
	public interface FeatureFlag {
		boolean isEnabled(String scopeIdentity, String workflow, String stage);
	}

	/**
	 * Kontrak callback/strategi bersarang milik {@link EbisnisMigrationOperationalControl}. Tipe ini memisahkan
	 * satu variasi perilaku lokal tanpa membuat service atau interface global yang tumpang tindih.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link EbisnisMigrationOperationalControl} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code authenticateAndAuthorize}(). Aturan
	 * bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see EbisnisMigrationOperationalControl
	 */
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
