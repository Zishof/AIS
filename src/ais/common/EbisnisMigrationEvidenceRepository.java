package ais.common;

import java.io.IOException;
import java.util.List;

/** Port penyimpanan evidence migrasi yang dapat diganti tanpa mengubah gate. */
public interface EbisnisMigrationEvidenceRepository {

	EbisnisMigrationEvidenceJournal.Entry append(
			EbisnisMigrationEvidenceJournal.Request request) throws IOException;

	List<EbisnisMigrationEvidenceJournal.Entry> read() throws IOException;

	EbisnisMigrationEvidenceJournal.Verification verify() throws IOException;
}
