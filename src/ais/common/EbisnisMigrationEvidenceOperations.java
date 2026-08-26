package ais.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Backup, restore, replay, dan retention-plan untuk evidence F17. */
public final class EbisnisMigrationEvidenceOperations {

	public static final class Snapshot {
		public final File file;
		public final File manifestFile;
		public final int recordCount;
		public final String lastHash;
		public final String fileHash;

		private Snapshot(File file, File manifestFile, int recordCount,
				String lastHash, String fileHash) {
			this.file = file;
			this.manifestFile = manifestFile;
			this.recordCount = recordCount;
			this.lastHash = lastHash;
			this.fileHash = fileHash;
		}
	}

	public static final class ReplaySummary {
		public final int recordCount;
		public final int prepared;
		public final int applied;
		public final int failed;
		public final int incomplete;

		private ReplaySummary(int recordCount, int prepared, int applied,
				int failed, int incomplete) {
			this.recordCount = recordCount;
			this.prepared = prepared;
			this.applied = applied;
			this.failed = failed;
			this.incomplete = incomplete;
		}
	}

	public static final class RetentionDecision {
		public final File file;
		public final boolean eligible;
		public final String reason;

		private RetentionDecision(File file, boolean eligible, String reason) {
			this.file = file;
			this.eligible = eligible;
			this.reason = reason;
		}
	}

	private EbisnisMigrationEvidenceOperations() {
	}

	/** Membuat snapshot baru tanpa pernah menimpa snapshot lama. */
	public static Snapshot seal(FileEbisnisMigrationEvidenceRepository source,
			File snapshotDirectory, String snapshotId) throws IOException {
		if (source == null) throw new IllegalArgumentException("source wajib diisi");
		if (snapshotDirectory == null) throw new IllegalArgumentException("snapshotDirectory wajib diisi");
		String safeId = safe(snapshotId, "snapshotId");
		EbisnisMigrationEvidenceJournal.Verification verification = source.verify();
		if (!verification.valid) throw new IOException("Evidence sumber rusak: " + verification.message);
		File directory = snapshotDirectory.getCanonicalFile();
		if (!directory.exists() && !directory.mkdirs()) {
			throw new IOException("Direktori snapshot tidak dapat dibuat");
		}
		File target = inside(directory, safeId + ".journal.snapshot");
		File manifest = inside(directory, safeId + ".manifest");
		if (!target.createNewFile()) throw new IOException("Snapshot sudah ada: " + safeId);
		boolean complete = false;
		try {
			copy(source.getJournalFile(), target);
			String fileHash = sha256(target);
			writeNew(manifest, "version=1\nrecords=" + verification.recordCount
					+ "\nlastHash=" + verification.lastHash + "\nfileHash="
					+ fileHash + "\n");
			complete = true;
			return new Snapshot(target, manifest, verification.recordCount,
					verification.lastHash, fileHash);
		} finally {
			if (!complete) {
				if (manifest.exists()) manifest.delete();
				if (target.exists()) target.delete();
			}
		}
	}

	/** Restore hanya diizinkan ke journal baru/kosong dan memverifikasi ulang. */
	public static void restore(Snapshot snapshot, File targetJournal)
			throws IOException {
		if (snapshot == null) throw new IllegalArgumentException("snapshot wajib diisi");
		if (targetJournal == null) throw new IllegalArgumentException("targetJournal wajib diisi");
		if (targetJournal.exists() && targetJournal.length() > 0L) {
			throw new IOException("Target restore harus baru atau kosong");
		}
		if (!snapshot.fileHash.equals(sha256(snapshot.file))) {
			throw new IOException("Hash file snapshot tidak cocok");
		}
		copy(snapshot.file, targetJournal);
		EbisnisMigrationEvidenceJournal.Verification verification =
				EbisnisMigrationEvidenceJournal.verify(targetJournal);
		if (!verification.valid || verification.recordCount != snapshot.recordCount
				|| !verification.lastHash.equals(snapshot.lastHash)) {
			throw new IOException("Hasil restore tidak identik dengan snapshot");
		}
	}

	public static ReplaySummary replay(EbisnisMigrationEvidenceRepository repository)
			throws IOException {
		if (repository == null) throw new IllegalArgumentException("repository wajib diisi");
		EbisnisMigrationEvidenceJournal.Verification verification = repository.verify();
		if (!verification.valid) throw new IOException("Evidence rusak: " + verification.message);
		List<EbisnisMigrationEvidenceJournal.Entry> entries = repository.read();
		Map<String, String> operationStates = new HashMap<String, String>();
		int prepared = 0;
		int applied = 0;
		int failed = 0;
		for (int i = 0; i < entries.size(); i++) {
			EbisnisMigrationEvidenceJournal.Entry entry = entries.get(i);
			String stage = entry.stage;
			if ("PREPARED".equals(stage)) {
				prepared++;
				String operationId = operationId(entry.eventId, ":PREPARED");
				if (operationId != null && !operationStates.containsKey(operationId)) {
					operationStates.put(operationId, "PREPARED");
				}
			} else if ("APPLIED".equals(stage)) {
				applied++;
				String operationId = operationId(entry.eventId, ":APPLIED");
				if (operationId != null) operationStates.put(operationId, "APPLIED");
			} else if ("FAILED".equals(stage)) {
				failed++;
				String operationId = operationId(entry.eventId, ":FAILED");
				if (operationId != null && !"APPLIED".equals(operationStates.get(operationId))) {
					operationStates.put(operationId, "FAILED");
				}
			}
		}
		int incomplete = 0;
		Iterator<String> states = operationStates.values().iterator();
		while (states.hasNext()) {
			if ("PREPARED".equals(states.next())) incomplete++;
		}
		return new ReplaySummary(entries.size(), prepared, applied, failed,
				incomplete);
	}

	private static String operationId(String eventId, String suffix) {
		if (eventId == null || suffix == null || !eventId.endsWith(suffix)) {
			return null;
		}
		return eventId.substring(0, eventId.length() - suffix.length());
	}

	/** Hanya membuat rencana; penghapusan fisik harus lewat approval terpisah. */
	public static RetentionDecision retention(File snapshot, long now,
			long minimumAgeMillis, boolean legalHold) {
		if (snapshot == null) throw new IllegalArgumentException("snapshot wajib diisi");
		if (minimumAgeMillis < 0L) throw new IllegalArgumentException("minimumAgeMillis tidak boleh negatif");
		if (legalHold) return new RetentionDecision(snapshot, false, "LEGAL_HOLD");
		long age = now - snapshot.lastModified();
		if (age < minimumAgeMillis) return new RetentionDecision(snapshot, false, "RETENTION_ACTIVE");
		return new RetentionDecision(snapshot, true, "ELIGIBLE_REQUIRES_APPROVAL");
	}

	private static File inside(File root, String name) throws IOException {
		File file = new File(root, name).getCanonicalFile();
		if (!file.getPath().startsWith(root.getPath() + File.separator)) {
			throw new IOException("Path keluar dari direktori evidence");
		}
		return file;
	}

	private static String safe(String value, String name) {
		if (value == null || value.trim().length() == 0) throw new IllegalArgumentException(name + " wajib diisi");
		return value.trim().replaceAll("[^A-Za-z0-9._-]", "_");
	}

	private static void copy(File source, File target) throws IOException {
		if (source == null || !source.isFile()) {
			throw new IOException("File sumber evidence tidak tersedia");
		}
		File parent = target.getCanonicalFile().getParentFile();
		if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IOException("Direktori target tidak dapat dibuat");
		FileInputStream input = null;
		FileOutputStream output = null;
		try {
			output = new FileOutputStream(target, false);
			input = new FileInputStream(source);
			byte[] buffer = new byte[8192];
			int read;
			while ((read = input.read(buffer)) >= 0) output.write(buffer, 0, read);
			output.flush();
			output.getFD().sync();
		} finally {
			if (input != null) try { input.close(); } catch (IOException ignored) { }
			if (output != null) try { output.close(); } catch (IOException ignored) { }
		}
	}

	private static void writeNew(File target, String content) throws IOException {
		if (!target.createNewFile()) throw new IOException("Manifest sudah ada");
		FileOutputStream output = null;
		try {
			output = new FileOutputStream(target, false);
			output.write(content.getBytes("UTF-8"));
			output.flush();
			output.getFD().sync();
		} finally {
			if (output != null) try { output.close(); } catch (IOException ignored) { }
		}
	}

	private static String sha256(File file) throws IOException {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			FileInputStream input = null;
			try {
				input = new FileInputStream(file);
				byte[] buffer = new byte[8192];
				int read;
				while ((read = input.read(buffer)) >= 0) digest.update(buffer, 0, read);
			} finally {
				if (input != null) try { input.close(); } catch (IOException ignored) { }
			}
			byte[] bytes = digest.digest();
			StringBuilder result = new StringBuilder();
			for (int i = 0; i < bytes.length; i++) {
				String hex = Integer.toHexString(bytes[i] & 0xff);
				if (hex.length() == 1) result.append('0');
				result.append(hex);
			}
			return result.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("SHA-256 tidak tersedia", e);
		}
	}
}
