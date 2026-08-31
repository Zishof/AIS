package ais.common;

/**
 * Orkestrator satu siklus snapshot F18. Penjadwal nyata memanggil runOnce;
 * kelas ini tidak membuat thread dan selalu mengirim alarm ketika gagal.
 */
public final class EbisnisMigrationEvidenceScheduler {

	/**
	 * Kontrak callback/strategi bersarang milik {@link EbisnisMigrationEvidenceScheduler}. Tipe ini memisahkan
	 * satu variasi perilaku lokal tanpa membuat service atau interface global yang tumpang tindih.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link EbisnisMigrationEvidenceScheduler} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code snapshot}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see EbisnisMigrationEvidenceScheduler
	 */
	public interface SnapshotSource {
		byte[] snapshot(String scopeIdentity) throws Exception;
	}

	/**
	 * Kontrak callback/strategi bersarang milik {@link EbisnisMigrationEvidenceScheduler}. Tipe ini memisahkan
	 * satu variasi perilaku lokal tanpa membuat service atau interface global yang tumpang tindih.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link EbisnisMigrationEvidenceScheduler} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code failed}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see EbisnisMigrationEvidenceScheduler
	 */
	public interface AlarmSink {
		void failed(String scopeIdentity, String code, String message);
	}

	/**
	 * Pembawa data/helper lokal milik {@link EbisnisMigrationEvidenceScheduler} untuk result. Tipe ini
	 * mengelompokkan nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang
	 * jelas.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * EbisnisMigrationEvidenceScheduler}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String scopeIdentity}, {@code String
	 * objectKey}, {@code String sha256}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
	 * dipanggilnya.</p>
	 *
	 * @see EbisnisMigrationEvidenceScheduler
	 */
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
