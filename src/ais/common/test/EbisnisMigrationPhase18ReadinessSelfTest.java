package ais.common.test;

import ais.common.EbisnisMigrationAuditedIdentityProvider;
import ais.common.EbisnisMigrationEvidenceScheduler;
import ais.common.EbisnisMigrationImmutableEvidencePublisher;
import ais.common.EbisnisMigrationOperationalControl;
import ais.common.EbisnisMigrationProductionCanaryGate;
import java.util.HashMap;
import java.util.Map;

/** UAT mandiri F18 tanpa framework dan kompatibel Java 1.7. */
public final class EbisnisMigrationPhase18ReadinessSelfTest {

	private static int assertions;

	private EbisnisMigrationPhase18ReadinessSelfTest() {
	}

	public static void main(String[] args) throws Exception {
		testIdentityAdapter();
		testImmutablePublisher();
		testSnapshotScheduler();
		testProductionCanary();
		System.out.println("OK F18 assertions=" + assertions);
	}

	private static void testIdentityAdapter() {
		final long now = 100000L;
		final EbisnisMigrationAuditedIdentityProvider.Decision[] audited =
				new EbisnisMigrationAuditedIdentityProvider.Decision[1];
		EbisnisMigrationAuditedIdentityProvider provider = provider(now,
				"operator", now + 1000L,
				new String[] { "ROLLOUT:CANARY" }, audited, false);
		check(provider.authenticateAndAuthorize("operator", "valid",
				"ROLLOUT", "CANARY"), "identitas valid diizinkan");
		check(audited[0] != null && audited[0].authorized,
				"keputusan identitas diaudit");
		equal("AUTHORIZED", audited[0].reason, "alasan identitas valid");

		provider = provider(now, "operator", now,
				new String[] { "ROLLOUT:*" }, audited, false);
		check(!provider.authenticateAndAuthorize("operator", "valid",
				"ROLLOUT", "CANARY"), "assertion kedaluwarsa ditolak");
		equal("ASSERTION_EXPIRED", audited[0].reason,
				"alasan assertion kedaluwarsa");

		provider = provider(now, "aktor-lain", now + 1000L,
				new String[] { "ROLLOUT:*" }, audited, false);
		check(!provider.authenticateAndAuthorize("operator", "valid",
				"ROLLOUT", "CANARY"), "aktor tidak cocok ditolak");
		equal("ACTOR_MISMATCH", audited[0].reason, "alasan aktor tidak cocok");

		provider = provider(now, "operator", now + 1000L,
				new String[] { "OTHER:*" }, audited, false);
		check(!provider.authenticateAndAuthorize("operator", "valid",
				"ROLLOUT", "CANARY"), "izin yang tidak cocok ditolak");
		equal("PERMISSION_DENIED", audited[0].reason, "alasan izin ditolak");

		provider = provider(now, "operator", now + 1000L,
				new String[] { "ROLLOUT:*" }, audited, true);
		check(!provider.authenticateAndAuthorize("operator", "valid",
				"ROLLOUT", "CANARY"), "kegagalan audit membuat fail-closed");
	}

	private static EbisnisMigrationAuditedIdentityProvider provider(
			final long now, final String principalActor, final long expiresAt,
			final String[] permissions,
			final EbisnisMigrationAuditedIdentityProvider.Decision[] audited,
			final boolean auditFails) {
		return new EbisnisMigrationAuditedIdentityProvider(
				new EbisnisMigrationAuditedIdentityProvider.CredentialVerifier() {
					public EbisnisMigrationAuditedIdentityProvider.Principal verify(
							String actor, String credential) {
						if (!"valid".equals(credential)) return null;
						return new EbisnisMigrationAuditedIdentityProvider.Principal(
								principalActor, "assertion-1", expiresAt,
								permissions);
					}
				}, new EbisnisMigrationAuditedIdentityProvider.AuditSink() {
					public void record(
							EbisnisMigrationAuditedIdentityProvider.Decision decision)
							throws Exception {
						if (auditFails) throw new Exception("audit gagal");
						audited[0] = decision;
					}
				}, new EbisnisMigrationAuditedIdentityProvider.TimeSource() {
					public long currentTimeMillis() { return now; }
				});
	}

	private static void testImmutablePublisher() throws Exception {
		MemoryStore store = new MemoryStore(true, false);
		EbisnisMigrationImmutableEvidencePublisher publisher =
				new EbisnisMigrationImmutableEvidencePublisher(store);
		EbisnisMigrationImmutableEvidencePublisher.Publication publication =
				publisher.publish("tenant/1/evidence", bytes("evidence"),
						System.currentTimeMillis() + 60000L);
		equal("tenant/1/evidence", publication.key, "key evidence");
		equal(64, publication.sha256.length(), "SHA-256 evidence");
		equal(8, publication.byteCount, "ukuran evidence");
		try {
			publisher.publish("tenant/1/evidence", bytes("duplikat"),
					System.currentTimeMillis() + 60000L);
			fail("object immutable tidak boleh ditimpa");
		} catch (IllegalStateException expected) {
			check(true, "putIfAbsent menolak duplikat");
		}

		try {
			new EbisnisMigrationImmutableEvidencePublisher(
					new MemoryStore(false, false)).publish("not-ready",
							bytes("evidence"), System.currentTimeMillis() + 60000L);
			fail("kapabilitas object store yang tidak lengkap wajib ditolak");
		} catch (IllegalStateException expected) {
			check(true, "object store belum siap ditolak");
		}

		try {
			new EbisnisMigrationImmutableEvidencePublisher(
					new MemoryStore(true, true)).publish("corrupt",
							bytes("evidence"), System.currentTimeMillis() + 60000L);
			fail("checksum read-back yang rusak wajib ditolak");
		} catch (IllegalStateException expected) {
			check(true, "checksum rusak ditolak");
		}
	}

	private static void testSnapshotScheduler() throws Exception {
		final int[] alarms = new int[] { 0 };
		EbisnisMigrationEvidenceScheduler scheduler = scheduler(false, alarms);
		EbisnisMigrationEvidenceScheduler.Result result = scheduler.runOnce(
				"tenant:1|lokasi:2", "snapshot-1",
				System.currentTimeMillis() + 60000L);
		equal("tenant:1|lokasi:2", result.scopeIdentity,
				"scope snapshot dipertahankan");
		equal(64, result.sha256.length(), "hash snapshot tersedia");
		equal(0, alarms[0], "tidak ada alarm untuk snapshot sukses");

		scheduler = scheduler(true, alarms);
		try {
			scheduler.runOnce("tenant:1|lokasi:2", "snapshot-gagal",
					System.currentTimeMillis() + 60000L);
			fail("kegagalan snapshot wajib diteruskan");
		} catch (Exception expected) {
			check(true, "kegagalan snapshot diteruskan");
		}
		equal(1, alarms[0], "kegagalan snapshot memicu alarm");
	}

	private static EbisnisMigrationEvidenceScheduler scheduler(
			final boolean fail, final int[] alarms) {
		return new EbisnisMigrationEvidenceScheduler(
				new EbisnisMigrationEvidenceScheduler.SnapshotSource() {
					public byte[] snapshot(String scopeIdentity) throws Exception {
						if (fail) throw new Exception("snapshot gagal");
						return bytes(scopeIdentity);
					}
				}, new EbisnisMigrationImmutableEvidencePublisher(
						new MemoryStore(true, false)),
				new EbisnisMigrationEvidenceScheduler.AlarmSink() {
					public void failed(String scopeIdentity, String code,
							String message) { alarms[0]++; }
				});
	}

	private static void testProductionCanary() {
		EbisnisMigrationProductionCanaryGate.Evidence complete = evidence(0L);
		EbisnisMigrationProductionCanaryGate.Decision disabled =
				EbisnisMigrationProductionCanaryGate.evaluate("tenant-1", "lokasi-1",
						EbisnisMigrationOperationalControl.DISABLED, complete);
		check(!disabled.allowed, "canary default OFF");
		check(disabled.reasons.size() == 1,
				"canary OFF memiliki alasan terukur");

		EbisnisMigrationOperationalControl.FeatureFlag enabled =
				new EbisnisMigrationOperationalControl.FeatureFlag() {
					public boolean isEnabled(String scopeIdentity, String workflow,
							String stage) {
						return "tenant-1|lokasi-1".equals(scopeIdentity)
								&& "MIGRATION".equals(workflow)
								&& "CANARY".equals(stage);
					}
				};
		EbisnisMigrationProductionCanaryGate.Decision allowed =
				EbisnisMigrationProductionCanaryGate.evaluate("tenant-1", "lokasi-1",
						enabled, complete);
		check(allowed.allowed, "canary lengkap diizinkan");
		equal(0, allowed.reasons.size(), "canary sukses tanpa alasan penolakan");

		EbisnisMigrationProductionCanaryGate.Decision mismatch =
				EbisnisMigrationProductionCanaryGate.evaluate("tenant-1", "lokasi-1",
						enabled, evidence(1L));
		check(!mismatch.allowed, "mismatch menutup canary");
		check(mismatch.reasons.contains("Rekonsiliasi masih memiliki mismatch"),
				"alasan mismatch eksplisit");
	}

	private static EbisnisMigrationProductionCanaryGate.Evidence evidence(
			long mismatch) {
		return new EbisnisMigrationProductionCanaryGate.Evidence(100L, mismatch,
				true, true, true, true, true, true);
	}

	/**
	 * Tipe implementasi bersarang {@link MemoryStore} milik {@link EbisnisMigrationPhase18ReadinessSelfTest}.
	 * Kelas ini memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok
	 * anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * EbisnisMigrationPhase18ReadinessSelfTest}. Dependensi yang diperlukan harus diberikan secara eksplisit agar
	 * aman digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai
	 * API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code boolean ready}, {@code boolean
	 * corruptRead}, {@code Map values}; operasi lokal: {@code capabilities()}, {@code putIfAbsent()}, {@code
	 * read}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see EbisnisMigrationPhase18ReadinessSelfTest
	 */
	private static final class MemoryStore implements
			EbisnisMigrationImmutableEvidencePublisher.ImmutableObjectStore {
		private final boolean ready;
		private final boolean corruptRead;
		private final Map<String, byte[]> values = new HashMap<String, byte[]>();

		private MemoryStore(boolean ready, boolean corruptRead) {
			this.ready = ready;
			this.corruptRead = corruptRead;
		}

		public EbisnisMigrationImmutableEvidencePublisher.Capabilities capabilities() {
			return new EbisnisMigrationImmutableEvidencePublisher.Capabilities(
					ready, ready, ready, ready, ready);
		}

		public void putIfAbsent(String key, byte[] payload, String sha256,
				long retainUntilMillis) {
			if (values.containsKey(key)) {
				throw new IllegalStateException("object sudah ada");
			}
			byte[] copy = new byte[payload.length];
			System.arraycopy(payload, 0, copy, 0, payload.length);
			values.put(key, copy);
		}

		public byte[] read(String key) {
			if (corruptRead) return bytes("rusak");
			return values.get(key);
		}
	}

	private static byte[] bytes(String value) {
		try {
			return value.getBytes("UTF-8");
		} catch (Exception impossible) {
			throw new IllegalStateException(impossible);
		}
	}

	private static void check(boolean value, String message) {
		assertions++;
		if (!value) throw new AssertionError(message);
	}

	private static void equal(Object expected, Object actual, String message) {
		assertions++;
		if (expected == null ? actual != null : !expected.equals(actual)) {
			throw new AssertionError(message + ": expected=" + expected
					+ ", actual=" + actual);
		}
	}

	private static void fail(String message) {
		throw new AssertionError(message);
	}
}
