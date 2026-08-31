package ais.common;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Journal append-only untuk evidence rollout dan dekomisioning eBisnis.
 *
 * <p>Setiap record dihubungkan dengan SHA-256 record sebelumnya. Kelas ini
 * menyediakan integritas dan deteksi manipulasi, bukan enkripsi. Payload wajib
 * sudah disanitasi dan tidak boleh berisi password, token, atau data pribadi
 * yang tidak diperlukan.</p>
 */
public final class EbisnisMigrationEvidenceJournal {

	public static final String WORKFLOW_ROLLOUT = "ROLLOUT";
	public static final String WORKFLOW_DECOMMISSION = "DECOMMISSION";
	private static final String FORMAT_VERSION = "1";
	private static final String ZERO_HASH =
			"0000000000000000000000000000000000000000000000000000000000000000";
	private static final int COLUMN_COUNT = 14;

	private EbisnisMigrationEvidenceJournal() {
	}

	/**
	 * Tipe implementasi bersarang {@link Request} milik {@link EbisnisMigrationEvidenceJournal}. Kelas ini memberi
	 * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * EbisnisMigrationEvidenceJournal}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code long occurredAt}, {@code String
	 * eventId}, {@code String workflow}, {@code String scopeIdentity}, {@code String stage}, {@code String
	 * decisionCode}, {@code String actor}, {@code String reference}. Aturan bisnis bersama tetap berada pada kelas
	 * induk atau service yang dipanggilnya.</p>
	 *
	 * @see EbisnisMigrationEvidenceJournal
	 */
	public static final class Request {
		public final long occurredAt;
		public final String eventId;
		public final String workflow;
		public final String scopeIdentity;
		public final String stage;
		public final String decisionCode;
		public final String actor;
		public final String reference;
		public final String evidencePayload;

		public Request(long occurredAt, String eventId, String workflow,
				String scopeIdentity, String stage, String decisionCode,
				String actor, String reference, String evidencePayload) {
			if (occurredAt < 0L) {
				throw new IllegalArgumentException("occurredAt tidak boleh negatif");
			}
			this.occurredAt = occurredAt;
			this.eventId = wajib(eventId, "eventId");
			this.workflow = wajib(workflow, "workflow");
			if (!WORKFLOW_ROLLOUT.equals(workflow)
					&& !WORKFLOW_DECOMMISSION.equals(workflow)) {
				throw new IllegalArgumentException("workflow tidak dikenal: " + workflow);
			}
			this.scopeIdentity = wajib(scopeIdentity, "scopeIdentity");
			this.stage = wajib(stage, "stage");
			this.decisionCode = wajib(decisionCode, "decisionCode");
			this.actor = wajib(actor, "actor");
			this.reference = wajib(reference, "reference");
			this.evidencePayload = wajib(evidencePayload, "evidencePayload");
		}
	}

	/**
	 * Tipe implementasi bersarang {@link Entry} milik {@link EbisnisMigrationEvidenceJournal}. Kelas ini memberi
	 * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * EbisnisMigrationEvidenceJournal}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code long sequence}, {@code long
	 * occurredAt}, {@code String eventId}, {@code String workflow}, {@code String scopeIdentity}, {@code String
	 * stage}, {@code String decisionCode}, {@code String actor}. Aturan bisnis bersama tetap berada pada kelas
	 * induk atau service yang dipanggilnya.</p>
	 *
	 * @see EbisnisMigrationEvidenceJournal
	 */
	public static final class Entry {
		public final long sequence;
		public final long occurredAt;
		public final String eventId;
		public final String workflow;
		public final String scopeIdentity;
		public final String stage;
		public final String decisionCode;
		public final String actor;
		public final String reference;
		public final String evidencePayload;
		public final String payloadHash;
		public final String previousHash;
		public final String recordHash;

		private Entry(long sequence, Request request, String payloadHash,
				String previousHash, String recordHash) {
			this.sequence = sequence;
			this.occurredAt = request.occurredAt;
			this.eventId = request.eventId;
			this.workflow = request.workflow;
			this.scopeIdentity = request.scopeIdentity;
			this.stage = request.stage;
			this.decisionCode = request.decisionCode;
			this.actor = request.actor;
			this.reference = request.reference;
			this.evidencePayload = request.evidencePayload;
			this.payloadHash = payloadHash;
			this.previousHash = previousHash;
			this.recordHash = recordHash;
		}
	}

	/**
	 * Tipe implementasi bersarang {@link Verification} milik {@link EbisnisMigrationEvidenceJournal}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * EbisnisMigrationEvidenceJournal}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code boolean valid}, {@code int
	 * recordCount}, {@code int errorLine}, {@code String message}, {@code String lastHash}. Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see EbisnisMigrationEvidenceJournal
	 */
	public static final class Verification {
		public final boolean valid;
		public final int recordCount;
		public final int errorLine;
		public final String message;
		public final String lastHash;

		private Verification(boolean valid, int recordCount, int errorLine,
				String message, String lastHash) {
			this.valid = valid;
			this.recordCount = recordCount;
			this.errorLine = errorLine;
			this.message = message;
			this.lastHash = lastHash;
		}
	}

	/** Menambah record secara atomik. Replay event identik bersifat idempoten. */
	public static Entry append(File file, Request request) throws IOException {
		if (file == null) throw new IllegalArgumentException("file wajib diisi");
		if (request == null) throw new IllegalArgumentException("request wajib diisi");
		ensureParent(file);
		RandomAccessFile access = null;
		FileChannel channel = null;
		FileLock lock = null;
		try {
			access = new RandomAccessFile(file, "rw");
			channel = access.getChannel();
			lock = channel.lock();
			List<Entry> entries = readAndVerify(access);
			for (int i = 0; i < entries.size(); i++) {
				Entry existing = entries.get(i);
				if (existing.eventId.equals(request.eventId)) {
					if (!same(existing, request)) {
						throw new IllegalStateException(
								"eventId sudah dipakai dengan payload berbeda: "
								+ request.eventId);
					}
					return existing;
				}
			}
			long sequence = entries.size() + 1L;
			String previousHash = entries.isEmpty() ? ZERO_HASH
					: entries.get(entries.size() - 1).recordHash;
			String payloadHash = sha256(request.evidencePayload);
			String[] columns = columns(sequence, request, payloadHash, previousHash);
			String recordHash = sha256Canonical(columns);
			Entry result = new Entry(sequence, request, payloadHash, previousHash,
					recordHash);
			access.seek(access.length());
			access.writeBytes(join(columns, recordHash));
			access.writeByte('\n');
			access.getFD().sync();
			return result;
		} finally {
			if (lock != null) try { lock.release(); } catch (IOException ignored) { }
			if (channel != null) try { channel.close(); } catch (IOException ignored) { }
			if (access != null) try { access.close(); } catch (IOException ignored) { }
		}
	}

	/** Membaca snapshot immutable dan menolak journal yang rusak. */
	public static List<Entry> read(File file) throws IOException {
		if (file == null) throw new IllegalArgumentException("file wajib diisi");
		if (!file.exists()) return Collections.emptyList();
		RandomAccessFile access = null;
		FileChannel channel = null;
		FileLock lock = null;
		try {
			access = new RandomAccessFile(file, "r");
			channel = access.getChannel();
			lock = channel.lock(0L, Long.MAX_VALUE, true);
			return Collections.unmodifiableList(readAndVerify(access));
		} finally {
			if (lock != null) try { lock.release(); } catch (IOException ignored) { }
			if (channel != null) try { channel.close(); } catch (IOException ignored) { }
			if (access != null) try { access.close(); } catch (IOException ignored) { }
		}
	}

	/** Memverifikasi rantai tanpa mengubah file. */
	public static Verification verify(File file) throws IOException {
		if (file == null) throw new IllegalArgumentException("file wajib diisi");
		if (!file.exists()) {
			return new Verification(true, 0, 0, "Journal kosong", ZERO_HASH);
		}
		RandomAccessFile access = null;
		FileChannel channel = null;
		FileLock lock = null;
		try {
			access = new RandomAccessFile(file, "r");
			channel = access.getChannel();
			lock = channel.lock(0L, Long.MAX_VALUE, true);
			try {
				List<Entry> entries = readAndVerify(access);
				String lastHash = entries.isEmpty() ? ZERO_HASH
						: entries.get(entries.size() - 1).recordHash;
				return new Verification(true, entries.size(), 0,
						"Rantai evidence valid", lastHash);
			} catch (JournalCorruptException e) {
				return new Verification(false, e.validRecordCount, e.line,
						e.getMessage(), e.lastValidHash);
			}
		} finally {
			if (lock != null) try { lock.release(); } catch (IOException ignored) { }
			if (channel != null) try { channel.close(); } catch (IOException ignored) { }
			if (access != null) try { access.close(); } catch (IOException ignored) { }
		}
	}

	private static List<Entry> readAndVerify(RandomAccessFile access)
			throws IOException {
		long length = access.length();
		if (length == 0L) return new ArrayList<Entry>();
		access.seek(length - 1L);
		if (access.read() != '\n') {
			throw corrupt(1, 0, ZERO_HASH, "Record terakhir belum lengkap");
		}
		access.seek(0L);
		List<Entry> entries = new ArrayList<Entry>();
		String expectedPrevious = ZERO_HASH;
		String line;
		int lineNumber = 0;
		while ((line = access.readLine()) != null) {
			lineNumber++;
			if (line.length() == 0) {
				throw corrupt(lineNumber, entries.size(), expectedPrevious,
						"Baris kosong tidak diizinkan");
			}
			String[] values = line.split("\\t", -1);
			if (values.length != COLUMN_COUNT) {
				throw corrupt(lineNumber, entries.size(), expectedPrevious,
						"Jumlah kolom journal tidak valid");
			}
			try {
				if (!FORMAT_VERSION.equals(values[0])) {
					throw new IllegalArgumentException("Versi format tidak dikenal");
				}
				long sequence = Long.parseLong(values[1]);
				long occurredAt = Long.parseLong(values[2]);
				if (sequence != entries.size() + 1L) {
					throw new IllegalArgumentException("Urutan sequence terputus");
				}
				if (!expectedPrevious.equals(values[12])) {
					throw new IllegalArgumentException("Rantai previousHash terputus");
				}
				String[] hashColumns = new String[COLUMN_COUNT - 1];
				System.arraycopy(values, 0, hashColumns, 0, hashColumns.length);
				if (!sha256Canonical(hashColumns).equals(values[13])) {
					throw new IllegalArgumentException("recordHash tidak sesuai");
				}
				Request request = new Request(occurredAt, decode(values[3]),
						decode(values[4]), decode(values[5]), decode(values[6]),
						decode(values[7]), decode(values[8]), decode(values[9]),
						decode(values[10]));
				if (!sha256(request.evidencePayload).equals(values[11])) {
					throw new IllegalArgumentException("payloadHash tidak sesuai");
				}
				Entry entry = new Entry(sequence, request, values[11], values[12],
						values[13]);
				entries.add(entry);
				expectedPrevious = entry.recordHash;
			} catch (RuntimeException e) {
				throw corrupt(lineNumber, entries.size(), expectedPrevious,
						"Record journal rusak: " + e.getMessage());
			}
		}
		return entries;
	}

	private static String[] columns(long sequence, Request request,
			String payloadHash, String previousHash) {
		return new String[] { FORMAT_VERSION, String.valueOf(sequence),
				String.valueOf(request.occurredAt), encode(request.eventId),
				encode(request.workflow), encode(request.scopeIdentity),
				encode(request.stage), encode(request.decisionCode),
				encode(request.actor), encode(request.reference),
				encode(request.evidencePayload), payloadHash, previousHash };
	}

	private static boolean same(Entry entry, Request request) {
		return entry.occurredAt == request.occurredAt
				&& entry.eventId.equals(request.eventId)
				&& entry.workflow.equals(request.workflow)
				&& entry.scopeIdentity.equals(request.scopeIdentity)
				&& entry.stage.equals(request.stage)
				&& entry.decisionCode.equals(request.decisionCode)
				&& entry.actor.equals(request.actor)
				&& entry.reference.equals(request.reference)
				&& entry.evidencePayload.equals(request.evidencePayload);
	}

	private static String join(String[] values, String recordHash) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			if (i > 0) result.append('\t');
			result.append(values[i]);
		}
		result.append('\t').append(recordHash);
		return result.toString();
	}

	private static String sha256Canonical(String[] values) {
		StringBuilder canonical = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			canonical.append(values[i].length()).append(':').append(values[i]);
		}
		return sha256(canonical.toString());
	}

	private static String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] bytes = digest.digest(value.getBytes("UTF-8"));
			return toHex(bytes);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 tidak tersedia", e);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("UTF-8 tidak tersedia", e);
		}
	}

	private static String encode(String value) {
		try {
			return toHex(value.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("UTF-8 tidak tersedia", e);
		}
	}

	private static String decode(String value) {
		if ((value.length() & 1) != 0) {
			throw new IllegalArgumentException("Encoding hex ganjil");
		}
		byte[] bytes = new byte[value.length() / 2];
		for (int i = 0; i < bytes.length; i++) {
			int high = Character.digit(value.charAt(i * 2), 16);
			int low = Character.digit(value.charAt(i * 2 + 1), 16);
			if (high < 0 || low < 0) {
				throw new IllegalArgumentException("Encoding hex tidak valid");
			}
			bytes[i] = (byte) ((high << 4) + low);
		}
		try {
			return new String(bytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("UTF-8 tidak tersedia", e);
		}
	}

	private static String toHex(byte[] bytes) {
		char[] digits = "0123456789abcdef".toCharArray();
		char[] result = new char[bytes.length * 2];
		for (int i = 0; i < bytes.length; i++) {
			int value = bytes[i] & 0xff;
			result[i * 2] = digits[value >>> 4];
			result[i * 2 + 1] = digits[value & 0x0f];
		}
		return new String(result);
	}

	private static String wajib(String value, String name) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(name + " wajib diisi");
		}
		return value;
	}

	private static void ensureParent(File file) throws IOException {
		File parent = file.getAbsoluteFile().getParentFile();
		if (parent != null && !parent.exists() && !parent.mkdirs()
				&& !parent.exists()) {
			throw new IOException("Tidak dapat membuat directory journal: " + parent);
		}
	}

	private static JournalCorruptException corrupt(int line, int validCount,
			String lastHash, String message) {
		return new JournalCorruptException(line, validCount, lastHash, message);
	}

	/**
	 * Tipe implementasi bersarang {@link JournalCorruptException} milik {@link EbisnisMigrationEvidenceJournal}.
	 * Kelas ini memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok
	 * anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * EbisnisMigrationEvidenceJournal}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
	 * kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int line}, {@code int
	 * validRecordCount}, {@code String lastValidHash}. Aturan bisnis bersama tetap berada pada kelas induk atau
	 * service yang dipanggilnya.</p>
	 *
	 * @see EbisnisMigrationEvidenceJournal
	 */
	private static final class JournalCorruptException extends EOFException {
		private static final long serialVersionUID = 1L;
		private final int line;
		private final int validRecordCount;
		private final String lastValidHash;

		private JournalCorruptException(int line, int validRecordCount,
				String lastValidHash, String message) {
			super(message);
			this.line = line;
			this.validRecordCount = validRecordCount;
			this.lastValidHash = lastValidHash;
		}
	}
}
