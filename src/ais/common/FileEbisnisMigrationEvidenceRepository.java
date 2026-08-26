package ais.common;

import java.io.File;
import java.io.IOException;
import java.util.List;

/** Repository evidence durable berbasis journal file append-only. */
public final class FileEbisnisMigrationEvidenceRepository
		implements EbisnisMigrationEvidenceRepository {

	private final File journalFile;

	public FileEbisnisMigrationEvidenceRepository(File rootDirectory,
			String scopeIdentity) throws IOException {
		if (rootDirectory == null) {
			throw new IllegalArgumentException("rootDirectory wajib diisi");
		}
		if (scopeIdentity == null || scopeIdentity.trim().length() == 0) {
			throw new IllegalArgumentException("scopeIdentity wajib diisi");
		}
		File root = rootDirectory.getCanonicalFile();
		String safeName = scopeIdentity.trim().replaceAll("[^A-Za-z0-9._-]", "_");
		File candidate = new File(root, safeName + ".journal").getCanonicalFile();
		String rootPath = root.getPath();
		String candidatePath = candidate.getPath();
		if (!candidatePath.startsWith(rootPath + File.separator)) {
			throw new IOException("Lokasi journal keluar dari root evidence");
		}
		this.journalFile = candidate;
	}

	public EbisnisMigrationEvidenceJournal.Entry append(
			EbisnisMigrationEvidenceJournal.Request request) throws IOException {
		return EbisnisMigrationEvidenceJournal.append(journalFile, request);
	}

	public List<EbisnisMigrationEvidenceJournal.Entry> read()
			throws IOException {
		return EbisnisMigrationEvidenceJournal.read(journalFile);
	}

	public EbisnisMigrationEvidenceJournal.Verification verify()
			throws IOException {
		return EbisnisMigrationEvidenceJournal.verify(journalFile);
	}

	public File getJournalFile() {
		return journalFile;
	}
}
