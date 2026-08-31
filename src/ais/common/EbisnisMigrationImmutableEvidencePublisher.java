package ais.common;

import java.security.MessageDigest;

/** Kontrak publikasi evidence ke object storage immutable/WORM F18. */
public final class EbisnisMigrationImmutableEvidencePublisher {

	/**
	 * Kontrak callback/strategi bersarang milik {@link EbisnisMigrationImmutableEvidencePublisher}. Tipe ini
	 * memisahkan satu variasi perilaku lokal tanpa membuat service atau interface global yang tumpang tindih.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link EbisnisMigrationImmutableEvidencePublisher}
	 * dan dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code capabilities()}, {@code
	 * putIfAbsent()}, {@code read}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang
	 * dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see EbisnisMigrationImmutableEvidencePublisher
	 */
	public interface ImmutableObjectStore {
		Capabilities capabilities() throws Exception;
		void putIfAbsent(String key, byte[] payload, String sha256,
				long retainUntilMillis) throws Exception;
		byte[] read(String key) throws Exception;
	}

	/**
	 * Tipe implementasi bersarang {@link Capabilities} milik {@link EbisnisMigrationImmutableEvidencePublisher}.
	 * Kelas ini memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok
	 * anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * EbisnisMigrationImmutableEvidencePublisher}. Dependensi yang diperlukan harus diberikan secara eksplisit
	 * agar aman digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code boolean immutableWrite}, {@code
	 * boolean encryptionAtRest}, {@code boolean versioning}, {@code boolean retentionLock}, {@code boolean
	 * crossHostReplication}; operasi lokal: {@code productionReady}(). Aturan bisnis bersama tetap berada pada
	 * kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see EbisnisMigrationImmutableEvidencePublisher
	 */
	public static final class Capabilities {
		public final boolean immutableWrite;
		public final boolean encryptionAtRest;
		public final boolean versioning;
		public final boolean retentionLock;
		public final boolean crossHostReplication;

		public Capabilities(boolean immutableWrite, boolean encryptionAtRest,
				boolean versioning, boolean retentionLock,
				boolean crossHostReplication) {
			this.immutableWrite = immutableWrite;
			this.encryptionAtRest = encryptionAtRest;
			this.versioning = versioning;
			this.retentionLock = retentionLock;
			this.crossHostReplication = crossHostReplication;
		}

		public boolean productionReady() {
			return immutableWrite && encryptionAtRest && versioning
					&& retentionLock && crossHostReplication;
		}
	}

	/**
	 * Tipe implementasi bersarang {@link Publication} milik {@link EbisnisMigrationImmutableEvidencePublisher}.
	 * Kelas ini memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok
	 * anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * EbisnisMigrationImmutableEvidencePublisher}. Dependensi yang diperlukan harus diberikan secara eksplisit
	 * agar aman digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String key}, {@code String sha256},
	 * {@code int byteCount}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
	 * dipanggilnya.</p>
	 *
	 * @see EbisnisMigrationImmutableEvidencePublisher
	 */
	public static final class Publication {
		public final String key;
		public final String sha256;
		public final int byteCount;

		private Publication(String key, String sha256, int byteCount) {
			this.key = key;
			this.sha256 = sha256;
			this.byteCount = byteCount;
		}
	}

	private final ImmutableObjectStore store;

	public EbisnisMigrationImmutableEvidencePublisher(
			ImmutableObjectStore store) {
		if (store == null) throw new IllegalArgumentException("store wajib diisi");
		this.store = store;
	}

	public Publication publish(String key, byte[] payload,
			long retainUntilMillis) throws Exception {
		String safeKey = required(key, "key");
		if (payload == null || payload.length == 0) {
			throw new IllegalArgumentException("payload wajib diisi");
		}
		if (retainUntilMillis <= System.currentTimeMillis()) {
			throw new IllegalArgumentException("retensi harus berada di masa depan");
		}
		Capabilities capabilities = store.capabilities();
		if (capabilities == null || !capabilities.productionReady()) {
			throw new IllegalStateException(
					"Object store belum memenuhi kontrak immutable produksi");
		}
		byte[] copy = new byte[payload.length];
		System.arraycopy(payload, 0, copy, 0, payload.length);
		String hash = sha256(copy);
		store.putIfAbsent(safeKey, copy, hash, retainUntilMillis);
		byte[] stored = store.read(safeKey);
		if (stored == null || !hash.equals(sha256(stored))) {
			throw new IllegalStateException("Checksum evidence hasil publikasi berbeda");
		}
		return new Publication(safeKey, hash, copy.length);
	}

	private static String sha256(byte[] value) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] bytes = digest.digest(value);
		StringBuilder result = new StringBuilder(bytes.length * 2);
		for (int i = 0; i < bytes.length; i++) {
			String hex = Integer.toHexString(bytes[i] & 0xff);
			if (hex.length() == 1) result.append('0');
			result.append(hex);
		}
		return result.toString();
	}

	private static String required(String value, String name) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(name + " wajib diisi");
		}
		return value.trim();
	}
}
