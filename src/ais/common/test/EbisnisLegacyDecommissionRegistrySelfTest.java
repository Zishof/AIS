package ais.common.test;

import java.util.List;

import ais.common.EbisnisLegacyDecommissionRegistry;

/** Self-test Java 1.7 untuk stabilisasi dan dekomisioning Fase 14. */
public final class EbisnisLegacyDecommissionRegistrySelfTest {

	private static int pemeriksaan;

	private EbisnisLegacyDecommissionRegistrySelfTest() {
	}

	private static void benar(boolean value, String context) {
		pemeriksaan++;
		if (!value) throw new IllegalStateException(context);
	}

	private static void sama(String expected, String actual, String context) {
		benar(expected.equals(actual), context + ": harapan=" + expected
				+ ", aktual=" + actual);
	}

	private static EbisnisLegacyDecommissionRegistry.Scope scope() {
		return new EbisnisLegacyDecommissionRegistry.Scope("inventory",
				"legacy_stock_writer", "warehouse-team", "release-2027-01");
	}

	private static EbisnisLegacyDecommissionRegistry.Evidence complete() {
		return new EbisnisLegacyDecommissionRegistry.Builder()
				.observationDays(45).errorRateBasisPoints(10).openAlertCount(0)
				.reconciliationExceptionCount(0).activeReaderCount(0)
				.activeWriterCount(0).monitoringStable(true)
				.routesDeprecated(true).actionsDeprecated(true)
				.tablesDeprecated(true).legacyWriterStopped(true)
				.mappingArchived(true).migrationAuditArchived(true)
				.backupVerified(true).restoreTestPassed(true)
				.replayTestPassed(true).sopUpdated(true)
				.trainingCompleted(true).runbookUpdated(true)
				.disasterRecoveryUpdated(true).ownerAssigned(true)
				.technicalApproved(true).businessApproved(true)
				.operationsApproved(true).securityApproved(true)
				.separateRemovalRelease(true).rollbackReady(true)
				.physicalRemovalVerified(true)
				.postRemovalMonitoringStable(true).build();
	}

	private static EbisnisLegacyDecommissionRegistry.Decision evaluate(
			String current, String requested,
			EbisnisLegacyDecommissionRegistry.Evidence evidence) {
		return EbisnisLegacyDecommissionRegistry.evaluate(scope(), current,
				requested, true,
				EbisnisLegacyDecommissionRegistry.conservativePolicy(), evidence);
	}

	private static void invalid(Runnable runnable, String context) {
		boolean failed = false;
		try { runnable.run(); } catch (IllegalArgumentException e) { failed = true; }
		benar(failed, context);
	}

	public static void main(String[] args) {
		EbisnisLegacyDecommissionRegistry.validate();
		benar(!EbisnisLegacyDecommissionRegistry.DEFAULT_DECOMMISSION_ENABLED,
				"dekomisioning default wajib nonaktif");
		List<String> stages = EbisnisLegacyDecommissionRegistry.orderedStages();
		benar(stages.size() == 7, "tujuh tahap aktif");
		sama(EbisnisLegacyDecommissionRegistry.OBSERVATION, stages.get(0),
				"observasi tahap pertama");
		sama(EbisnisLegacyDecommissionRegistry.COMPLETE, stages.get(6),
				"complete tahap terakhir");

		EbisnisLegacyDecommissionRegistry.Decision disabled =
				EbisnisLegacyDecommissionRegistry.evaluate(scope(), stages.get(0),
						stages.get(1), false,
						EbisnisLegacyDecommissionRegistry.conservativePolicy(), complete());
		benar(!disabled.allowed, "flag nonaktif memblokir");
		sama(EbisnisLegacyDecommissionRegistry.BLOCKED_DISABLED, disabled.code,
				"kode disabled stabil");

		for (int i = 0; i < stages.size() - 1; i++) {
			EbisnisLegacyDecommissionRegistry.Decision result = evaluate(
					stages.get(i), stages.get(i + 1), complete());
			benar(result.allowed, "transisi sah: " + stages.get(i));
			sama(EbisnisLegacyDecommissionRegistry.ALLOWED, result.code,
					"kode transisi sah: " + stages.get(i));
			benar(result.missingEvidence.isEmpty(),
					"transisi sehat tanpa evidence hilang");
		}

		EbisnisLegacyDecommissionRegistry.Decision noChange = evaluate(
				stages.get(0), stages.get(0), complete());
		benar(noChange.allowed, "tahap sama idempoten");
		sama(EbisnisLegacyDecommissionRegistry.NO_CHANGE, noChange.code,
				"kode no change stabil");

		EbisnisLegacyDecommissionRegistry.Decision skip = evaluate(stages.get(0),
				stages.get(2), complete());
		benar(!skip.allowed, "tahap tidak boleh dilompati");
		sama(EbisnisLegacyDecommissionRegistry.BLOCKED_SEQUENCE, skip.code,
				"kode sequence stabil");

		EbisnisLegacyDecommissionRegistry.Evidence shortObservation =
				new EbisnisLegacyDecommissionRegistry.Builder()
						.observationDays(29).monitoringStable(true)
						.reconciliationExceptionCount(0).build();
		EbisnisLegacyDecommissionRegistry.Decision observation = evaluate(
				stages.get(0), stages.get(1), shortObservation);
		benar(!observation.allowed, "observasi kurang diblokir");
		benar(observation.missingEvidence.contains("observationDays"),
				"durasi observasi dilaporkan");

		EbisnisLegacyDecommissionRegistry.Evidence unstable =
				new EbisnisLegacyDecommissionRegistry.Builder()
						.observationDays(30).monitoringStable(false)
						.errorRateBasisPoints(26).openAlertCount(1).build();
		EbisnisLegacyDecommissionRegistry.Decision monitoring = evaluate(
				stages.get(0), stages.get(1), unstable);
		sama(EbisnisLegacyDecommissionRegistry.BLOCKED_MONITORING,
				monitoring.code, "monitoring tidak stabil dibedakan");
		benar(monitoring.missingEvidence.size() == 3,
				"seluruh masalah monitoring dilaporkan");

		EbisnisLegacyDecommissionRegistry.Evidence dependency =
				new EbisnisLegacyDecommissionRegistry.Builder()
						.activeReaderCount(1).activeWriterCount(1)
						.monitoringStable(true).backupVerified(true)
						.restoreTestPassed(true).replayTestPassed(true)
						.sopUpdated(true).trainingCompleted(true)
						.runbookUpdated(true).disasterRecoveryUpdated(true)
						.ownerAssigned(true).separateRemovalRelease(true)
						.technicalApproved(true).businessApproved(true)
						.operationsApproved(true).securityApproved(true).build();
		EbisnisLegacyDecommissionRegistry.Decision depended = evaluate(
				stages.get(4), stages.get(5), dependency);
		sama(EbisnisLegacyDecommissionRegistry.BLOCKED_DEPENDENCY, depended.code,
				"reader/writer aktif memblokir penghapusan");

		EbisnisLegacyDecommissionRegistry.Evidence noSignoff =
				new EbisnisLegacyDecommissionRegistry.Builder()
						.monitoringStable(true).backupVerified(true)
						.restoreTestPassed(true).replayTestPassed(true)
						.sopUpdated(true).trainingCompleted(true)
						.runbookUpdated(true).disasterRecoveryUpdated(true)
						.ownerAssigned(true).separateRemovalRelease(true).build();
		EbisnisLegacyDecommissionRegistry.Decision signoff = evaluate(
				stages.get(4), stages.get(5), noSignoff);
		sama(EbisnisLegacyDecommissionRegistry.BLOCKED_SIGN_OFF, signoff.code,
				"sign-off wajib lengkap");
		benar(signoff.missingEvidence.size() == 4,
				"empat sign-off dilaporkan");

		EbisnisLegacyDecommissionRegistry.Evidence incident =
				new EbisnisLegacyDecommissionRegistry.Builder()
						.dataIntegrityIncident(true).rollbackReady(true).build();
		EbisnisLegacyDecommissionRegistry.Decision rollback = evaluate(
				stages.get(3), stages.get(4), incident);
		benar(!rollback.allowed && rollback.rollbackRequired,
				"insiden memaksa rollback");
		sama(EbisnisLegacyDecommissionRegistry.ROLLBACK_REQUIRED, rollback.code,
				"kode insiden stabil");

		EbisnisLegacyDecommissionRegistry.Decision explicitRollback = evaluate(
				stages.get(3), EbisnisLegacyDecommissionRegistry.ROLLED_BACK,
				complete());
		benar(explicitRollback.allowed && explicitRollback.rollbackRequired,
				"rollback eksplisit diizinkan bila siap");

		invalid(new Runnable() { public void run() {
			new EbisnisLegacyDecommissionRegistry.Scope("", "writer", "owner", "release");
		} }, "scope kosong ditolak");
		invalid(new Runnable() { public void run() {
			new EbisnisLegacyDecommissionRegistry.Policy(0, 0, 0);
		} }, "policy observasi nol ditolak");
		invalid(new Runnable() { public void run() {
			new EbisnisLegacyDecommissionRegistry.Builder().activeWriterCount(-1);
		} }, "counter negatif ditolak");
		invalid(new Runnable() { public void run() {
			EbisnisLegacyDecommissionRegistry.evaluate(scope(), "UNKNOWN",
					EbisnisLegacyDecommissionRegistry.COMPLETE, true,
					EbisnisLegacyDecommissionRegistry.conservativePolicy(), complete());
		} }, "tahap asing ditolak");

		benar("inventory/legacy_stock_writer/release-2027-01".equals(
				evaluate(stages.get(0), stages.get(1), complete()).scopeIdentity),
				"identitas scope dapat diaudit");
		System.out.println("LULUS: " + pemeriksaan + " pemeriksaan Fase 14");
	}
}
