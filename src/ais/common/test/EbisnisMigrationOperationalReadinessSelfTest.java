package ais.common.test;

import ais.common.EbisnisMigrationEvidenceGate;
import ais.common.EbisnisMigrationEvidenceJournal;
import ais.common.EbisnisMigrationEvidenceOperations;
import ais.common.EbisnisMigrationEvidenceRepository;
import ais.common.EbisnisMigrationOperationalControl;
import ais.common.FileEbisnisMigrationEvidenceRepository;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** UAT mandiri F17 tanpa framework dan kompatibel Java 1.7. */
public final class EbisnisMigrationOperationalReadinessSelfTest {

	private static int assertions;

	private EbisnisMigrationOperationalReadinessSelfTest() {
	}

	public static void main(String[] args) throws Exception {
		File root = new File(System.getProperty("java.io.tmpdir"),
				"ebisnis-f17-" + System.nanoTime());
		check(root.mkdirs(), "direktori UAT dibuat");
		try {
			testFailClosed(root);
			testExecuteSnapshotRestoreReplay(root);
			testConcurrencyAndCrashRecovery(root);
			testFailureAndRetention(root);
			System.out.println("OK F17 assertions=" + assertions);
		} finally {
			delete(root);
		}
	}

	private static void testFailClosed(File root) throws Exception {
		FileEbisnisMigrationEvidenceRepository repository = repository(root,
				"closed.journal");
		final int[] calls = new int[] { 0 };
		EbisnisMigrationOperationalControl control = control(repository, false,
				true);
		try {
			control.execute(plan("closed-1"), "valid",
					action(calls, false));
			fail("flag default OFF wajib menolak");
		} catch (EbisnisMigrationEvidenceGate.GateException e) {
			equal("ROLLOUT_DISABLED", e.code, "kode flag OFF");
		}
		equal(0, calls[0], "aksi tidak dijalankan saat flag OFF");
		equal(0, repository.read().size(), "tidak ada evidence saat flag OFF");

		control = control(repository, true, false);
		try {
			control.execute(plan("closed-2"), "salah",
					action(calls, false));
			fail("identitas salah wajib ditolak");
		} catch (EbisnisMigrationEvidenceGate.GateException e) {
			equal("IDENTITY_NOT_AUTHORIZED", e.code, "kode identitas ditolak");
		}
		equal(0, calls[0], "aksi tidak dijalankan saat identitas ditolak");
	}

	private static void testExecuteSnapshotRestoreReplay(File root)
			throws Exception {
		FileEbisnisMigrationEvidenceRepository repository = repository(root,
				"success.journal");
		EbisnisMigrationOperationalControl control = control(repository, true,
				true);
		final int[] calls = new int[] { 0 };
		EbisnisMigrationEvidenceGate.Result first = control.execute(
				plan("success-1"), "valid", action(calls, false));
		equal(EbisnisMigrationEvidenceGate.RESULT_APPLIED, first.status,
				"operasi pertama applied");
		EbisnisMigrationEvidenceGate.Result second = control.execute(
				plan("success-1"), "valid", action(calls, false));
		equal(EbisnisMigrationEvidenceGate.RESULT_ALREADY_APPLIED, second.status,
				"replay dideteksi idempotent");
		equal(1, calls[0], "aksi idempotent hanya sekali");
		equal(2, repository.read().size(), "PREPARED dan APPLIED tersimpan");

		EbisnisMigrationEvidenceOperations.Snapshot snapshot =
				EbisnisMigrationEvidenceOperations.seal(repository,
						new File(root, "snapshots"), "success-1");
		check(snapshot.file.exists(), "snapshot tersedia");
		check(snapshot.manifestFile.exists(), "manifest tersedia");
		equal(2, snapshot.recordCount, "jumlah record snapshot");
		check(snapshot.fileHash.length() == 64, "hash snapshot SHA-256");
		try {
			EbisnisMigrationEvidenceOperations.seal(repository,
					new File(root, "snapshots"), "success-1");
			fail("snapshot tidak boleh ditimpa");
		} catch (IOException expected) {
			check(true, "snapshot immutable");
		}

		File restored = new File(root, "restored.journal");
		EbisnisMigrationEvidenceOperations.restore(snapshot, restored);
		EbisnisMigrationEvidenceJournal.Verification verification =
				EbisnisMigrationEvidenceJournal.verify(restored);
		check(verification.valid, "hasil restore valid");
		equal(2, verification.recordCount, "hasil restore identik");
		EbisnisMigrationEvidenceOperations.ReplaySummary replay =
				EbisnisMigrationEvidenceOperations.replay(repository);
		equal(2, replay.recordCount, "replay membaca semua record");
		equal(1, replay.prepared, "replay PREPARED");
		equal(1, replay.applied, "replay APPLIED");
		equal(0, replay.incomplete, "tidak ada operasi menggantung");

		File nonEmpty = new File(root, "non-empty.journal");
		write(nonEmpty, "isi");
		try {
			EbisnisMigrationEvidenceOperations.restore(snapshot, nonEmpty);
			fail("restore tidak boleh menimpa journal berisi");
		} catch (IOException expected) {
			check(true, "restore fail-closed");
		}
		snapshot.file.delete();
		try {
			EbisnisMigrationEvidenceOperations.restore(snapshot,
					new File(root, "missing-source.journal"));
			fail("restore wajib menolak snapshot yang hilang");
		} catch (IOException expected) {
			check(true, "snapshot hilang ditolak");
		}
	}

	private static void testConcurrencyAndCrashRecovery(File root)
			throws Exception {
		final FileEbisnisMigrationEvidenceRepository repository = repository(root,
				"concurrent.journal");
		final EbisnisMigrationOperationalControl control = control(repository,
				true, true);
		final int[] calls = new int[] { 0 };
		final List<Throwable> failures = new ArrayList<Throwable>();
		List<Thread> threads = new ArrayList<Thread>();
		for (int i = 0; i < 8; i++) {
			Thread thread = new Thread(new Runnable() {
				public void run() {
					try {
						control.execute(plan("concurrent-1"), "valid",
								action(calls, false));
					} catch (Throwable e) {
						synchronized (failures) { failures.add(e); }
					}
				}
			});
			threads.add(thread);
			thread.start();
		}
		for (int i = 0; i < threads.size(); i++) threads.get(i).join();
		equal(0, failures.size(), "konkurensi tanpa kegagalan");
		equal(1, calls[0], "konkurensi menjalankan aksi sekali");
		equal(2, repository.read().size(), "konkurensi tidak menggandakan evidence");

		EbisnisMigrationEvidenceJournal.Request prepared =
				new EbisnisMigrationEvidenceJournal.Request(1L,
						"crash-1:PREPARED",
						EbisnisMigrationEvidenceJournal.WORKFLOW_ROLLOUT,
						"tenant:1|unit:2", "PREPARED", "AUTHORIZED",
						"operator", "ref-crash", "payload");
		repository.append(prepared);
		final int[] recoveredCalls = new int[] { 0 };
		EbisnisMigrationEvidenceGate.Result recovered = control.execute(
				plan("crash-1"), "valid", action(recoveredCalls, false));
		equal(EbisnisMigrationEvidenceGate.RESULT_APPLIED, recovered.status,
				"PREPARED sisa crash dapat dilanjutkan");
		equal(1, recoveredCalls[0], "recovery menjalankan aksi sekali");
		EbisnisMigrationEvidenceOperations.ReplaySummary replay =
				EbisnisMigrationEvidenceOperations.replay(repository);
		equal(0, replay.incomplete, "recovery menutup evidence menggantung");
	}

	private static void testFailureAndRetention(File root) throws Exception {
		FileEbisnisMigrationEvidenceRepository repository = repository(root,
				"failed.journal");
		EbisnisMigrationOperationalControl control = control(repository, true,
				true);
		try {
			control.execute(plan("failed-1"), "valid",
					action(new int[] { 0 }, true));
			fail("aksi gagal wajib dilempar");
		} catch (EbisnisMigrationEvidenceGate.GateException e) {
			equal("GUARDED_ACTION_FAILED", e.code, "kode aksi gagal");
		}
		EbisnisMigrationEvidenceOperations.ReplaySummary replay =
				EbisnisMigrationEvidenceOperations.replay(repository);
		equal(1, replay.failed, "FAILED tercatat");
		equal(0, replay.incomplete, "FAILED bukan operasi menggantung");

		File snapshot = new File(root, "retention.snapshot");
		write(snapshot, "evidence");
		long now = System.currentTimeMillis();
		snapshot.setLastModified(now - 10000L);
		check(!EbisnisMigrationEvidenceOperations.retention(snapshot, now,
				1000L, true).eligible, "legal hold mencegah retensi");
		check(!EbisnisMigrationEvidenceOperations.retention(snapshot, now,
				20000L, false).eligible, "umur aktif mencegah retensi");
		EbisnisMigrationEvidenceOperations.RetentionDecision eligible =
				EbisnisMigrationEvidenceOperations.retention(snapshot, now,
						1000L, false);
		check(eligible.eligible, "snapshot tua eligible setelah approval");
		equal("ELIGIBLE_REQUIRES_APPROVAL", eligible.reason,
				"retensi tetap membutuhkan approval");
	}

	private static EbisnisMigrationOperationalControl control(
			EbisnisMigrationEvidenceRepository repository,
			final boolean enabled, final boolean authorized) {
		return new EbisnisMigrationOperationalControl(repository,
				new EbisnisMigrationOperationalControl.FeatureFlag() {
					public boolean isEnabled(String scopeIdentity,
							String workflow, String stage) { return enabled; }
				}, new EbisnisMigrationOperationalControl.IdentityProvider() {
					public boolean authenticateAndAuthorize(String actor,
							String credential, String workflow, String stage) {
						return authorized && "valid".equals(credential);
					}
				});
	}

	private static EbisnisMigrationEvidenceGate.Plan plan(String operationId) {
		return new EbisnisMigrationEvidenceGate.Plan(operationId,
				EbisnisMigrationEvidenceJournal.WORKFLOW_ROLLOUT,
				"tenant:1|unit:2", "CANARY", "operator", "ref-1", "payload");
	}

	private static EbisnisMigrationEvidenceGate.GuardedAction action(
			final int[] calls, final boolean fail) {
		return new EbisnisMigrationEvidenceGate.GuardedAction() {
			public void execute(String operationId) throws Exception {
				synchronized (calls) { calls[0]++; }
				if (fail) throw new IOException("simulasi gagal");
			}
		};
	}

	private static FileEbisnisMigrationEvidenceRepository repository(File root,
			String name) throws IOException {
		String scope = name;
		if (scope.endsWith(".journal")) {
			scope = scope.substring(0, scope.length() - ".journal".length());
		}
		return new FileEbisnisMigrationEvidenceRepository(root, scope);
	}

	private static void write(File file, String value) throws IOException {
		FileOutputStream output = null;
		try {
			output = new FileOutputStream(file);
			output.write(value.getBytes("UTF-8"));
		} finally {
			if (output != null) try { output.close(); } catch (IOException ignored) { }
		}
	}

	private static void check(boolean value, String message) {
		assertions++;
		if (!value) throw new AssertionError(message);
	}

	private static void equal(int expected, int actual, String message) {
		check(expected == actual, message + " expected=" + expected + " actual=" + actual);
	}

	private static void equal(String expected, String actual, String message) {
		check(expected.equals(actual), message + " expected=" + expected + " actual=" + actual);
	}

	private static void fail(String message) {
		throw new AssertionError(message);
	}

	private static void delete(File file) {
		if (file == null || !file.exists()) return;
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children != null) for (int i = 0; i < children.length; i++) delete(children[i]);
		}
		file.delete();
	}
}
