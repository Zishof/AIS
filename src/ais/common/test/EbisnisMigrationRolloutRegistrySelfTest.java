package ais.common.test;

import java.util.List;

import ais.common.EbisnisMigrationRolloutRegistry;

/** Self-test Java 1.7 untuk gerbang migrasi dan rollout Fase 13. */
public final class EbisnisMigrationRolloutRegistrySelfTest {

	private static int pemeriksaan;

	private EbisnisMigrationRolloutRegistrySelfTest() {
	}

	private static void benar(boolean value, String context) {
		pemeriksaan++;
		if (!value) {
			throw new IllegalStateException(context);
		}
	}

	private static void sama(String expected, String actual, String context) {
		benar(expected.equals(actual), context + ": harapan=" + expected
				+ ", aktual=" + actual);
	}

	private static EbisnisMigrationRolloutRegistry.Scope scope(int canary) {
		return new EbisnisMigrationRolloutRegistry.Scope("tenant-demo",
				"gudang-utama", "inventory_movement", canary);
	}

	private static EbisnisMigrationRolloutRegistry.Evidence completeEvidence() {
		return new EbisnisMigrationRolloutRegistry.Builder()
				.baselineCaptured(true).dryRunPassed(true)
				.backfillPassed(true).shadowReadPassed(true)
				.shadowWritePassed(true).reconciliationPassed(true)
				.canaryPassed(true).cutoverStable(true)
				.rollbackRehearsed(true).qaApproved(true)
				.businessApproved(true).financeApproved(true)
				.warehouseApproved(true).itApproved(true)
				.checkedRecords(10000L).mismatchCount(0L)
				.errorRateBasisPoints(10).latencyRegressionPercent(5)
				.observationMinutes(2880).build();
	}

	private static EbisnisMigrationRolloutRegistry.Decision evaluate(
			String current, String requested,
			EbisnisMigrationRolloutRegistry.Evidence evidence) {
		return EbisnisMigrationRolloutRegistry.evaluate(scope(5), current,
				requested, true,
				EbisnisMigrationRolloutRegistry.conservativePolicy(), evidence);
	}

	private static void invalid(Runnable runnable, String context) {
		boolean failed = false;
		try {
			runnable.run();
		} catch (IllegalArgumentException e) {
			failed = true;
		}
		benar(failed, context);
	}

	public static void main(String[] args) {
		EbisnisMigrationRolloutRegistry.validate();
		benar(!EbisnisMigrationRolloutRegistry.DEFAULT_ROLLOUT_ENABLED,
				"rollout default wajib nonaktif");
		List<String> stages = EbisnisMigrationRolloutRegistry.orderedStages();
		benar(stages.size() == 9, "sembilan tahap aktif terdaftar");
		sama(EbisnisMigrationRolloutRegistry.BASELINE, stages.get(0),
				"baseline tahap pertama");
		sama(EbisnisMigrationRolloutRegistry.COMPLETE, stages.get(8),
				"complete tahap terakhir");

		EbisnisMigrationRolloutRegistry.Decision disabled =
				EbisnisMigrationRolloutRegistry.evaluate(scope(5),
						EbisnisMigrationRolloutRegistry.BASELINE,
						EbisnisMigrationRolloutRegistry.DRY_RUN, false,
						EbisnisMigrationRolloutRegistry.conservativePolicy(),
						completeEvidence());
		benar(!disabled.allowed, "flag nonaktif memblokir rollout");
		sama(EbisnisMigrationRolloutRegistry.BLOCKED_DISABLED, disabled.code,
				"kode flag nonaktif stabil");

		EbisnisMigrationRolloutRegistry.Decision noChange = evaluate(
				EbisnisMigrationRolloutRegistry.BASELINE,
				EbisnisMigrationRolloutRegistry.BASELINE, completeEvidence());
		benar(noChange.allowed, "evaluasi tahap yang sama idempoten");
		sama(EbisnisMigrationRolloutRegistry.NO_CHANGE, noChange.code,
				"kode no change stabil");

		for (int i = 0; i < stages.size() - 1; i++) {
			EbisnisMigrationRolloutRegistry.Decision decision = evaluate(
					stages.get(i), stages.get(i + 1), completeEvidence());
			benar(decision.allowed, "transisi sah: " + stages.get(i));
			sama(EbisnisMigrationRolloutRegistry.ALLOWED, decision.code,
					"kode transisi sah: " + stages.get(i));
			benar(!decision.rollbackRequired,
					"transisi sehat tidak meminta rollback: " + stages.get(i));
			sama("tenant-demo/gudang-utama/inventory_movement",
					decision.scopeIdentity, "scope audit stabil");
		}

		EbisnisMigrationRolloutRegistry.Decision skipped = evaluate(
				EbisnisMigrationRolloutRegistry.BASELINE,
				EbisnisMigrationRolloutRegistry.BACKFILL, completeEvidence());
		benar(!skipped.allowed, "tahap tidak boleh dilompati");
		sama(EbisnisMigrationRolloutRegistry.BLOCKED_SEQUENCE, skipped.code,
				"kode lompatan tahap stabil");

		EbisnisMigrationRolloutRegistry.Evidence empty =
				new EbisnisMigrationRolloutRegistry.Builder().build();
		EbisnisMigrationRolloutRegistry.Decision dryRun = evaluate(
				EbisnisMigrationRolloutRegistry.BASELINE,
				EbisnisMigrationRolloutRegistry.DRY_RUN, empty);
		benar(!dryRun.allowed && dryRun.reasons.size() == 2,
				"dry run membutuhkan baseline dan rollback rehearsal");

		EbisnisMigrationRolloutRegistry.Decision reconciliation = evaluate(
				EbisnisMigrationRolloutRegistry.SHADOW_WRITE,
				EbisnisMigrationRolloutRegistry.RECONCILIATION,
				new EbisnisMigrationRolloutRegistry.Builder()
						.shadowWritePassed(true).build());
		benar(!reconciliation.allowed,
				"rekonsiliasi tanpa record wajib ditolak");

		EbisnisMigrationRolloutRegistry.Decision canaryTooLarge =
				EbisnisMigrationRolloutRegistry.evaluate(scope(11),
						EbisnisMigrationRolloutRegistry.RECONCILIATION,
						EbisnisMigrationRolloutRegistry.CANARY, true,
						EbisnisMigrationRolloutRegistry.conservativePolicy(),
						completeEvidence());
		benar(!canaryTooLarge.allowed,
				"canary di atas batas policy wajib ditolak");

		EbisnisMigrationRolloutRegistry.Evidence mismatch =
				new EbisnisMigrationRolloutRegistry.Builder()
						.shadowReadPassed(true).checkedRecords(1000L)
						.mismatchCount(1L).build();
		EbisnisMigrationRolloutRegistry.Decision mismatchDecision = evaluate(
				EbisnisMigrationRolloutRegistry.SHADOW_READ,
				EbisnisMigrationRolloutRegistry.SHADOW_WRITE, mismatch);
		benar(!mismatchDecision.allowed && mismatchDecision.rollbackRequired,
				"mismatch memicu rollback sebelum shadow write");
		sama(EbisnisMigrationRolloutRegistry.ROLLBACK_REQUIRED,
				mismatchDecision.code, "kode mismatch stabil");

		EbisnisMigrationRolloutRegistry.Evidence incident =
				new EbisnisMigrationRolloutRegistry.Builder()
						.reconciliationPassed(true).rollbackRehearsed(true)
						.qaApproved(true).itApproved(true)
						.dataIntegrityIncident(true).build();
		EbisnisMigrationRolloutRegistry.Decision incidentDecision = evaluate(
				EbisnisMigrationRolloutRegistry.RECONCILIATION,
				EbisnisMigrationRolloutRegistry.CANARY, incident);
		benar(incidentDecision.rollbackRequired,
				"insiden integritas data memicu rollback");

		EbisnisMigrationRolloutRegistry.Evidence errorRate =
				new EbisnisMigrationRolloutRegistry.Builder()
						.canaryPassed(true).rollbackRehearsed(true)
						.qaApproved(true).businessApproved(true)
						.financeApproved(true).warehouseApproved(true)
						.itApproved(true).checkedRecords(1000L)
						.errorRateBasisPoints(51).observationMinutes(1440)
						.build();
		EbisnisMigrationRolloutRegistry.Decision unhealthy = evaluate(
				EbisnisMigrationRolloutRegistry.CANARY,
				EbisnisMigrationRolloutRegistry.CUTOVER, errorRate);
		benar(!unhealthy.allowed && unhealthy.rollbackRequired,
				"error rate di atas batas memicu rollback");

		EbisnisMigrationRolloutRegistry.Evidence shortObservation =
				new EbisnisMigrationRolloutRegistry.Builder()
						.canaryPassed(true).qaApproved(true)
						.businessApproved(true).financeApproved(true)
						.warehouseApproved(true).itApproved(true)
						.observationMinutes(1439).build();
		EbisnisMigrationRolloutRegistry.Decision observation = evaluate(
				EbisnisMigrationRolloutRegistry.CANARY,
				EbisnisMigrationRolloutRegistry.CUTOVER,
				shortObservation);
		benar(!observation.allowed && !observation.rollbackRequired,
				"observasi pendek memblokir tanpa memaksa rollback");

		EbisnisMigrationRolloutRegistry.Decision rollback = evaluate(
				EbisnisMigrationRolloutRegistry.CANARY,
				EbisnisMigrationRolloutRegistry.ROLLED_BACK,
				completeEvidence());
		benar(rollback.allowed, "rollback eksplisit dari canary diizinkan");

		EbisnisMigrationRolloutRegistry.Decision rollbackWithoutRehearsal =
				evaluate(EbisnisMigrationRolloutRegistry.CANARY,
						EbisnisMigrationRolloutRegistry.ROLLED_BACK, empty);
		benar(!rollbackWithoutRehearsal.allowed
				&& rollbackWithoutRehearsal.rollbackRequired,
				"rollback tanpa rehearsal tetap ditandai wajib");

		invalid(new Runnable() {
			public void run() {
				new EbisnisMigrationRolloutRegistry.Scope("", "gudang", "writer", 5);
			}
		}, "scope kosong ditolak");
		invalid(new Runnable() {
			public void run() {
				new EbisnisMigrationRolloutRegistry.Scope("tenant", "gudang", "writer", 101);
			}
		}, "canary di atas 100 ditolak");
		invalid(new Runnable() {
			public void run() {
				new EbisnisMigrationRolloutRegistry.Policy(-1L, 0, 0, 1, 1);
			}
		}, "policy negatif ditolak");
		invalid(new Runnable() {
			public void run() {
				new EbisnisMigrationRolloutRegistry.Builder().mismatchCount(-1L)
						.build();
			}
		}, "evidence negatif ditolak");
		invalid(new Runnable() {
			public void run() {
				EbisnisMigrationRolloutRegistry.evaluate(scope(5), "UNKNOWN",
						EbisnisMigrationRolloutRegistry.DRY_RUN, true,
						EbisnisMigrationRolloutRegistry.conservativePolicy(),
						completeEvidence());
			}
		}, "tahap asing ditolak");

		System.out.println("Self-test EbisnisMigrationRolloutRegistry: LULUS ("
				+ pemeriksaan + " pemeriksaan)");
	}
}
