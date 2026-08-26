package ais.common.test;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.List;

import ais.common.EbisnisMigrationEvidenceJournal;

/** Self-test Java 1.7 untuk journal evidence Fase 15. */
public final class EbisnisMigrationEvidenceJournalSelfTest {

	private static int pemeriksaan;

	private EbisnisMigrationEvidenceJournalSelfTest() {
	}

	private static void benar(boolean value, String context) {
		pemeriksaan++;
		if (!value) throw new IllegalStateException(context);
	}

	private static void sama(String expected, String actual, String context) {
		benar(expected.equals(actual), context + ": harapan=" + expected
				+ ", aktual=" + actual);
	}

	private static EbisnisMigrationEvidenceJournal.Request request(String id,
			String workflow, String stage, String payload) {
		return new EbisnisMigrationEvidenceJournal.Request(1787788800000L, id,
				workflow, "tenant-1/location-2/writer-pos", stage, "ALLOWED",
				"operator-uat", "change-2026-08", payload);
	}

	private static void invalid(Runnable runnable, String context) {
		boolean failed = false;
		try { runnable.run(); } catch (IllegalArgumentException e) { failed = true; }
		benar(failed, context);
	}

	public static void main(String[] args) throws Exception {
		File file = File.createTempFile("ebisnis-evidence-", ".journal");
		if (!file.delete()) throw new IllegalStateException("temp tidak dapat disiapkan");
		try {
			EbisnisMigrationEvidenceJournal.Verification empty =
					EbisnisMigrationEvidenceJournal.verify(file);
			benar(empty.valid, "journal belum ada valid");
			benar(empty.recordCount == 0, "journal awal kosong");

			EbisnisMigrationEvidenceJournal.Request rollout = request("evt-001",
					EbisnisMigrationEvidenceJournal.WORKFLOW_ROLLOUT, "BASELINE",
					"{\"reconciliationMismatch\":0}");
			EbisnisMigrationEvidenceJournal.Entry first =
					EbisnisMigrationEvidenceJournal.append(file, rollout);
			benar(first.sequence == 1L, "sequence pertama");
			sama("evt-001", first.eventId, "event pertama");
			benar(first.recordHash.length() == 64, "hash record SHA-256");
			benar(first.payloadHash.length() == 64, "hash payload SHA-256");

			EbisnisMigrationEvidenceJournal.Entry second =
					EbisnisMigrationEvidenceJournal.append(file, request("evt-002",
							EbisnisMigrationEvidenceJournal.WORKFLOW_DECOMMISSION,
							"OBSERVATION", "observasi=45;alert=0;rahasia=tidak-ada"));
			benar(second.sequence == 2L, "sequence kedua");
			sama(first.recordHash, second.previousHash, "rantai previous hash");

			List<EbisnisMigrationEvidenceJournal.Entry> entries =
					EbisnisMigrationEvidenceJournal.read(file);
			benar(entries.size() == 2, "dua record terbaca");
			sama("ROLLOUT", entries.get(0).workflow, "workflow pertama");
			sama("DECOMMISSION", entries.get(1).workflow, "workflow kedua");
			sama("{\"reconciliationMismatch\":0}",
					entries.get(0).evidencePayload, "payload round trip UTF-8");
			boolean immutable = false;
			try { entries.clear(); } catch (UnsupportedOperationException e) {
				immutable = true;
			}
			benar(immutable, "snapshot hasil baca immutable");

			EbisnisMigrationEvidenceJournal.Entry replay =
					EbisnisMigrationEvidenceJournal.append(file, rollout);
			benar(replay.sequence == 1L, "replay identik mengembalikan record lama");
			benar(EbisnisMigrationEvidenceJournal.read(file).size() == 2,
					"replay tidak menambah record");

			boolean conflict = false;
			try {
				EbisnisMigrationEvidenceJournal.append(file, request("evt-001",
						EbisnisMigrationEvidenceJournal.WORKFLOW_ROLLOUT, "BASELINE",
						"payload-berbeda"));
			} catch (IllegalStateException e) { conflict = true; }
			benar(conflict, "event ID konflik ditolak");

			EbisnisMigrationEvidenceJournal.Verification verified =
					EbisnisMigrationEvidenceJournal.verify(file);
			benar(verified.valid, "rantai dua record valid");
			benar(verified.recordCount == 2, "jumlah verifikasi");
			sama(second.recordHash, verified.lastHash, "hash terakhir");

			RandomAccessFile tamper = new RandomAccessFile(file, "rw");
			try {
				tamper.seek(8L);
				int original = tamper.read();
				tamper.seek(8L);
				tamper.write(original == 'a' ? 'b' : 'a');
			} finally { tamper.close(); }
			EbisnisMigrationEvidenceJournal.Verification corrupted =
					EbisnisMigrationEvidenceJournal.verify(file);
			benar(!corrupted.valid, "tampering terdeteksi");
			benar(corrupted.errorLine > 0, "baris rusak dilaporkan");
			boolean appendBlocked = false;
			try {
				EbisnisMigrationEvidenceJournal.append(file, request("evt-003",
						EbisnisMigrationEvidenceJournal.WORKFLOW_ROLLOUT, "CANARY",
						"evidence=ok"));
			} catch (java.io.IOException e) { appendBlocked = true; }
			benar(appendBlocked, "append pada journal rusak ditolak");

			invalid(new Runnable() { public void run() {
				new EbisnisMigrationEvidenceJournal.Request(-1L, "evt", "ROLLOUT",
						"scope", "stage", "decision", "actor", "ref", "payload");
			} }, "timestamp negatif ditolak");
			invalid(new Runnable() { public void run() {
				new EbisnisMigrationEvidenceJournal.Request(1L, "evt", "LAIN",
						"scope", "stage", "decision", "actor", "ref", "payload");
			} }, "workflow asing ditolak");
			invalid(new Runnable() { public void run() {
				new EbisnisMigrationEvidenceJournal.Request(1L, " ", "ROLLOUT",
						"scope", "stage", "decision", "actor", "ref", "payload");
			} }, "event kosong ditolak");

			System.out.println("EbisnisMigrationEvidenceJournalSelfTest LULUS: "
					+ pemeriksaan + " pemeriksaan");
		} finally {
			if (file.exists() && !file.delete()) file.deleteOnExit();
		}
	}
}
