package ais.common.test;

import ais.common.EbisnisMigrationEvidenceGate;
import ais.common.EbisnisMigrationEvidenceJournal;
import ais.common.EbisnisMigrationEvidenceRepository;
import ais.common.FileEbisnisMigrationEvidenceRepository;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

/** UAT mandiri untuk repository durable dan evidence gate fail-closed. */
public final class EbisnisMigrationEvidenceGateSelfTest {

	private static int assertions;

	private EbisnisMigrationEvidenceGateSelfTest() {
	}

	public static void main(String[] args) throws Exception {
		File root = new File(System.getProperty("java.io.tmpdir"),
				"ebisnis-evidence-gate-" + System.currentTimeMillis());
		check(root.mkdirs(), "directory UAT dibuat");
		try {
			testSuccessRetryAndMetrics(root);
			testUnauthorized(root);
			testActionFailure(root);
			testEvidenceUnavailable();
			testTamperFailClosed(root);
			testRestoreReplay(root);
			testScopeIsolation(root);
			System.out.println("OK - " + assertions + " assertions");
		} finally {
			deleteRecursively(root);
		}
	}

	private static void testSuccessRetryAndMetrics(File root) throws Exception {
		FileEbisnisMigrationEvidenceRepository repository = repository(root,
				"tenant-a|toko-1");
		EbisnisMigrationEvidenceGate gate = gate(repository, true);
		final int[] executions = new int[] { 0 };
		EbisnisMigrationEvidenceGate.Plan plan = plan("op-success", "actor-ok");
		EbisnisMigrationEvidenceGate.GuardedAction action =
				new EbisnisMigrationEvidenceGate.GuardedAction() {
			public void execute(String operationId) {
				executions[0]++;
			}
		};
		EbisnisMigrationEvidenceGate.Result first = gate.execute(plan, action);
		EbisnisMigrationEvidenceGate.Result retry = gate.execute(plan, action);
		check(EbisnisMigrationEvidenceGate.RESULT_APPLIED.equals(first.status),
				"eksekusi pertama APPLIED");
		check(EbisnisMigrationEvidenceGate.RESULT_ALREADY_APPLIED.equals(retry.status),
				"retry terdeteksi sudah applied");
		check(executions[0] == 1, "aksi tidak dijalankan dua kali");
		check(repository.read().size() == 2, "PREPARED dan APPLIED tercatat");
		check(repository.verify().valid, "journal sukses valid");
		EbisnisMigrationEvidenceGate.Metrics metrics = gate.metrics();
		check(metrics.attempts == 2L, "metrics attempt");
		check(metrics.applied == 1L, "metrics applied");
		check(metrics.alreadyApplied == 1L, "metrics retry");
	}

	private static void testUnauthorized(File root) throws Exception {
		FileEbisnisMigrationEvidenceRepository repository = repository(root,
				"tenant-b");
		EbisnisMigrationEvidenceGate gate = gate(repository, false);
		final int[] executions = new int[] { 0 };
		try {
			gate.execute(plan("op-rejected", "actor-no"), counting(executions));
			fail("actor tanpa hak harus ditolak");
		} catch (EbisnisMigrationEvidenceGate.GateException e) {
			check("ACTOR_NOT_AUTHORIZED".equals(e.code), "kode unauthorized");
		}
		check(executions[0] == 0, "aksi unauthorized tidak berjalan");
		check(repository.read().isEmpty(), "unauthorized tidak menulis evidence");
		check(gate.metrics().rejected == 1L, "metrics rejected");
	}

	private static void testActionFailure(File root) throws Exception {
		FileEbisnisMigrationEvidenceRepository repository = repository(root,
				"tenant-c");
		EbisnisMigrationEvidenceGate gate = gate(repository, true);
		try {
			gate.execute(plan("op-failed", "actor-ok"),
					new EbisnisMigrationEvidenceGate.GuardedAction() {
				public void execute(String operationId) throws Exception {
					throw new IOException("simulasi gagal");
				}
			});
			fail("action failure harus diteruskan");
		} catch (EbisnisMigrationEvidenceGate.GateException e) {
			check("GUARDED_ACTION_FAILED".equals(e.code), "kode action failure");
		}
		List<EbisnisMigrationEvidenceJournal.Entry> entries = repository.read();
		check(entries.size() == 2, "PREPARED dan FAILED tercatat");
		check("FAILED".equals(entries.get(1).stage), "tidak ada APPLIED palsu");
		check(gate.metrics().actionFailures == 1L, "metrics action failure");
	}

	private static void testEvidenceUnavailable() throws Exception {
		EbisnisMigrationEvidenceRepository broken =
				new EbisnisMigrationEvidenceRepository() {
			public EbisnisMigrationEvidenceJournal.Entry append(
					EbisnisMigrationEvidenceJournal.Request request) throws IOException {
				throw new IOException("disk gagal");
			}
			public List<EbisnisMigrationEvidenceJournal.Entry> read()
					throws IOException {
				throw new IOException("disk gagal");
			}
			public EbisnisMigrationEvidenceJournal.Verification verify()
					throws IOException {
				throw new IOException("disk gagal");
			}
		};
		EbisnisMigrationEvidenceGate gate = gate(broken, true);
		final int[] executions = new int[] { 0 };
		try {
			gate.execute(plan("op-storage", "actor-ok"), counting(executions));
			fail("repository gagal harus fail-closed");
		} catch (EbisnisMigrationEvidenceGate.GateException e) {
			check("EVIDENCE_UNAVAILABLE".equals(e.code), "kode evidence unavailable");
		}
		check(executions[0] == 0, "aksi tidak berjalan saat evidence unavailable");
		check(gate.metrics().evidenceFailures == 1L, "metrics evidence failure");
	}

	private static void testTamperFailClosed(File root) throws Exception {
		FileEbisnisMigrationEvidenceRepository repository = repository(root,
				"tenant-tamper");
		EbisnisMigrationEvidenceGate initial = gate(repository, true);
		initial.execute(plan("op-before-tamper", "actor-ok"),
				counting(new int[] { 0 }));
		RandomAccessFile access = new RandomAccessFile(repository.getJournalFile(), "rw");
		try {
			access.seek(10L);
			access.writeByte('X');
			access.getFD().sync();
		} finally {
			access.close();
		}
		final int[] executions = new int[] { 0 };
		EbisnisMigrationEvidenceGate gate = gate(repository, true);
		try {
			gate.execute(plan("op-after-tamper", "actor-ok"), counting(executions));
			fail("journal tamper harus ditolak");
		} catch (EbisnisMigrationEvidenceGate.GateException e) {
			check("EVIDENCE_CORRUPT".equals(e.code), "kode evidence corrupt");
		}
		check(executions[0] == 0, "aksi tidak berjalan setelah tamper");
	}

	private static void testRestoreReplay(File root) throws Exception {
		File sourceRoot = new File(root, "restore-source");
		File targetRoot = new File(root, "restore-target");
		FileEbisnisMigrationEvidenceRepository source = repository(sourceRoot,
				"tenant-restore");
		EbisnisMigrationEvidenceGate sourceGate = gate(source, true);
		sourceGate.execute(plan("op-restore", "actor-ok"), counting(new int[] { 0 }));
		check(targetRoot.mkdirs(), "target restore dibuat");
		FileEbisnisMigrationEvidenceRepository target = repository(targetRoot,
				"tenant-restore");
		copy(source.getJournalFile(), target.getJournalFile());
		check(target.verify().valid, "hasil restore valid");
		final int[] executions = new int[] { 0 };
		EbisnisMigrationEvidenceGate.Result result = gate(target, true).execute(
				plan("op-restore", "actor-ok"), counting(executions));
		check(EbisnisMigrationEvidenceGate.RESULT_ALREADY_APPLIED.equals(result.status),
				"replay restore mengenali APPLIED");
		check(executions[0] == 0, "restore tidak mengulang aksi");
	}

	private static void testScopeIsolation(File root) throws Exception {
		File isolatedRoot = new File(root, "isolated");
		FileEbisnisMigrationEvidenceRepository first = repository(isolatedRoot,
				"tenant/alpha");
		FileEbisnisMigrationEvidenceRepository second = repository(isolatedRoot,
				"tenant/beta");
		check(!first.getJournalFile().equals(second.getJournalFile()),
				"scope memakai file berbeda");
		check(first.getJournalFile().getParentFile().getCanonicalFile().equals(
				isolatedRoot.getCanonicalFile()), "journal tetap di root");
	}

	private static FileEbisnisMigrationEvidenceRepository repository(File root,
			String scope) throws Exception {
		if (!root.exists()) check(root.mkdirs(), "root repository dibuat");
		return new FileEbisnisMigrationEvidenceRepository(root, scope);
	}

	private static EbisnisMigrationEvidenceGate gate(
			EbisnisMigrationEvidenceRepository repository,
			final boolean authorized) {
		return new EbisnisMigrationEvidenceGate(repository,
				new EbisnisMigrationEvidenceGate.ActorAuthenticator() {
			public boolean isAuthorized(String actor, String workflow, String stage) {
				return authorized;
			}
		});
	}

	private static EbisnisMigrationEvidenceGate.Plan plan(String operationId,
			String actor) {
		return new EbisnisMigrationEvidenceGate.Plan(operationId,
				EbisnisMigrationEvidenceJournal.WORKFLOW_ROLLOUT,
				"tenant-demo|toko-1", "CUTOVER", actor, "CHG-16",
				"checksum=abc123;approval=approved");
	}

	private static EbisnisMigrationEvidenceGate.GuardedAction counting(
			final int[] executions) {
		return new EbisnisMigrationEvidenceGate.GuardedAction() {
			public void execute(String operationId) {
				executions[0]++;
			}
		};
	}

	private static void copy(File source, File target) throws IOException {
		RandomAccessFile input = null;
		RandomAccessFile output = null;
		try {
			input = new RandomAccessFile(source, "r");
			output = new RandomAccessFile(target, "rw");
			byte[] buffer = new byte[4096];
			int read;
			while ((read = input.read(buffer)) >= 0) {
				if (read > 0) output.write(buffer, 0, read);
			}
			output.getFD().sync();
		} finally {
			if (input != null) try { input.close(); } catch (IOException ignored) { }
			if (output != null) try { output.close(); } catch (IOException ignored) { }
		}
	}

	private static void deleteRecursively(File file) {
		if (file == null || !file.exists()) return;
		File[] children = file.listFiles();
		if (children != null) {
			for (int i = 0; i < children.length; i++) deleteRecursively(children[i]);
		}
		file.delete();
	}

	private static void check(boolean condition, String message) {
		assertions++;
		if (!condition) throw new AssertionError(message);
	}

	private static void fail(String message) {
		throw new AssertionError(message);
	}
}
