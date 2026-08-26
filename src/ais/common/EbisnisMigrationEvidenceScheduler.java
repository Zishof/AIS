package ais.common;

/**
 * Orkestrator satu siklus snapshot F18. Penjadwal nyata memanggil runOnce;
 * kelas ini tidak membuat thread dan selalu mengirim alarm ketika gagal.
 */
public final class EbisnisMigrationEvidenceScheduler {

	public interface SnapshotSource {
		byte[] snapshot(String scopeIdentity) throws Exception;
	}

	public interface AlarmSink {
		void failed(String scopeIdentity, String code, String message);
	}

	public static final class Result {
		public final String scopeIdentity;
		public final String objectKey;
		public final String sha256;

		private Result(String scopeIdentity, String objectKey, String sha256) {
			this.scopeIdentity = scopeIdentity;
			this.objectKey = objectKey;
			this.sha256 = sha256;
		}
	}

	private final SnapshotSource source;
	private final EbisnisMigrationImmutableEvidencePublisher publisher;
	private final AlarmSink alarmSink;

	public EbisnisMigrationEvidenceScheduler(SnapshotSource source,
			EbisnisMigrationImmutableEvidencePublisher publisher,
			AlarmSink alarmSink) {
		if (source == null) throw new IllegalArgumentException("source wajib diisi");
		if (publisher == null) throw new IllegalArgumentException("publisher wajib diisi");
		if (alarmSink == null) throw new IllegalArgumentException("alarmSink wajib diisi");
		this.source = source;
		this.publisher = publisher;
		this.alarmSink = alarmSink;
	}

	public Result runOnce(String scopeIdentity, String objectKey,
			long retainUntilMillis) throws Exception {
		try {
			byte[] snapshot = source.snapshot(scopeIdentity);
			EbisnisMigrationImmutableEvidencePublisher.Publication publication =
					publisher.publish(objectKey, snapshot, retainUntilMillis);
			return new Result(scopeIdentity, publication.key, publication.sha256);
		} catch (Exception failure) {
			try {
				alarmSink.failed(scopeIdentity, "EVIDENCE_SNAPSHOT_FAILED",
						failure.getMessage());
			} catch (RuntimeException ignored) {
				// Kegagalan alarm tidak boleh menyamarkan kegagalan utama.
			}
			throw failure;
		}
	}
}
