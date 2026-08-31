package ais.common;

/**
 * Adaptor identitas produksi F18. Verifikasi kredensial dan pencatatan audit
 * diserahkan kepada port infrastruktur nyata; keputusan selalu fail-closed.
 */
public final class EbisnisMigrationAuditedIdentityProvider
		implements EbisnisMigrationOperationalControl.IdentityProvider {

	/**
	 * Kontrak callback/strategi bersarang milik {@link EbisnisMigrationAuditedIdentityProvider}. Tipe ini
	 * memisahkan satu variasi perilaku lokal tanpa membuat service atau interface global yang tumpang tindih.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link EbisnisMigrationAuditedIdentityProvider} dan
	 * dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code verify}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see EbisnisMigrationAuditedIdentityProvider
	 */
	public interface CredentialVerifier {
		Principal verify(String actor, String credential) throws Exception;
	}

	/**
	 * Kontrak callback/strategi bersarang milik {@link EbisnisMigrationAuditedIdentityProvider}. Tipe ini
	 * memisahkan satu variasi perilaku lokal tanpa membuat service atau interface global yang tumpang tindih.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link EbisnisMigrationAuditedIdentityProvider} dan
	 * dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code record}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see EbisnisMigrationAuditedIdentityProvider
	 */
	public interface AuditSink {
		void record(Decision decision) throws Exception;
	}

	/**
	 * Kontrak callback/strategi bersarang milik {@link EbisnisMigrationAuditedIdentityProvider}. Tipe ini
	 * memisahkan satu variasi perilaku lokal tanpa membuat service atau interface global yang tumpang tindih.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link EbisnisMigrationAuditedIdentityProvider} dan
	 * dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code currentTimeMillis}(). Aturan bisnis
	 * bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see EbisnisMigrationAuditedIdentityProvider
	 */
	public interface TimeSource {
		long currentTimeMillis();
	}

	/**
	 * Tipe implementasi bersarang {@link Principal} milik {@link EbisnisMigrationAuditedIdentityProvider}. Kelas
	 * ini memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * EbisnisMigrationAuditedIdentityProvider}. Dependensi yang diperlukan harus diberikan secara eksplisit agar
	 * aman digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String actor}, {@code String
	 * assertionId}, {@code long expiresAtMillis}, {@code String permissions}; operasi lokal: {@code permits}().
	 * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see EbisnisMigrationAuditedIdentityProvider
	 */
	public static final class Principal {
		public final String actor;
		public final String assertionId;
		public final long expiresAtMillis;
		private final String[] permissions;

		public Principal(String actor, String assertionId, long expiresAtMillis,
				String[] permissions) {
			this.actor = required(actor, "actor");
			this.assertionId = required(assertionId, "assertionId");
			this.expiresAtMillis = expiresAtMillis;
			if (permissions == null) {
				throw new IllegalArgumentException("permissions wajib diisi");
			}
			this.permissions = new String[permissions.length];
			System.arraycopy(permissions, 0, this.permissions, 0,
					permissions.length);
		}

		public boolean permits(String workflow, String stage) {
			String exact = permission(workflow, stage);
			String workflowAll = required(workflow, "workflow") + ":*";
			for (int i = 0; i < permissions.length; i++) {
				if (exact.equals(permissions[i])
						|| workflowAll.equals(permissions[i])) return true;
			}
			return false;
		}
	}

	/**
	 * Tipe implementasi bersarang {@link Decision} milik {@link EbisnisMigrationAuditedIdentityProvider}. Kelas
	 * ini memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * EbisnisMigrationAuditedIdentityProvider}. Dependensi yang diperlukan harus diberikan secara eksplisit agar
	 * aman digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String actor}, {@code String
	 * assertionId}, {@code String workflow}, {@code String stage}, {@code boolean authorized}, {@code String
	 * reason}, {@code long decidedAtMillis}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
	 * dipanggilnya.</p>
	 *
	 * @see EbisnisMigrationAuditedIdentityProvider
	 */
	public static final class Decision {
		public final String actor;
		public final String assertionId;
		public final String workflow;
		public final String stage;
		public final boolean authorized;
		public final String reason;
		public final long decidedAtMillis;

		private Decision(String actor, String assertionId, String workflow,
				String stage, boolean authorized, String reason,
				long decidedAtMillis) {
			this.actor = actor;
			this.assertionId = assertionId;
			this.workflow = workflow;
			this.stage = stage;
			this.authorized = authorized;
			this.reason = reason;
			this.decidedAtMillis = decidedAtMillis;
		}
	}

	private final CredentialVerifier verifier;
	private final AuditSink auditSink;
	private final TimeSource timeSource;

	public EbisnisMigrationAuditedIdentityProvider(CredentialVerifier verifier,
			AuditSink auditSink, TimeSource timeSource) {
		if (verifier == null) throw new IllegalArgumentException("verifier wajib diisi");
		if (auditSink == null) throw new IllegalArgumentException("auditSink wajib diisi");
		if (timeSource == null) throw new IllegalArgumentException("timeSource wajib diisi");
		this.verifier = verifier;
		this.auditSink = auditSink;
		this.timeSource = timeSource;
	}

	public boolean authenticateAndAuthorize(String actor, String credential,
			String workflow, String stage) {
		long now = timeSource.currentTimeMillis();
		Principal principal = null;
		boolean authorized = false;
		String reason = "VERIFICATION_FAILED";
		try {
			principal = verifier.verify(actor, credential);
			if (principal == null) {
				reason = "IDENTITY_NOT_VERIFIED";
			} else if (!principal.actor.equals(actor)) {
				reason = "ACTOR_MISMATCH";
			} else if (principal.expiresAtMillis <= now) {
				reason = "ASSERTION_EXPIRED";
			} else if (!principal.permits(workflow, stage)) {
				reason = "PERMISSION_DENIED";
			} else {
				authorized = true;
				reason = "AUTHORIZED";
			}
		} catch (Exception ignored) {
			authorized = false;
			reason = "VERIFICATION_FAILED";
		}
		Decision decision = new Decision(actor,
				principal == null ? "-" : principal.assertionId, workflow, stage,
				authorized, reason, now);
		try {
			auditSink.record(decision);
		} catch (Exception ignored) {
			return false;
		}
		return authorized;
	}

	private static String permission(String workflow, String stage) {
		return required(workflow, "workflow") + ":"
				+ required(stage, "stage");
	}

	private static String required(String value, String name) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(name + " wajib diisi");
		}
		return value.trim();
	}
}
