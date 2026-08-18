package ais.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.database.hibernate.StreamingHibernateUtil;

public class CommonFileUtil {
	private static final long MINIMUM_FREE_BYTES = 20L * 1024L * 1024L;

	// Menggunakan AtomicLong agar aman (thread-safe) saat diakses banyak user
	// bersamaan
	private static final AtomicLong increments = new AtomicLong(0);

	/** Pastikan direktori tujuan ada dan filesystem masih mempunyai ruang kerja.
	 * Pemeriksaan dilakukan sebelum file kosong dibuat supaya kegagalan disk penuh
	 * tidak meninggalkan artefak 0-byte yang kemudian dianggap sebagai hasil valid. */
	public static void ensureWritableFile(File file, long estimatedBytes) throws IOException {
		if (file == null) {
			throw new IOException("Lokasi file keluaran tidak tersedia.");
		}
		File parent = file.getAbsoluteFile().getParentFile();
		if (parent == null) {
			throw new IOException("Direktori file keluaran tidak tersedia: " + file);
		}
		if (!parent.exists() && !parent.mkdirs() && !parent.exists()) {
			throw new IOException("Direktori file keluaran tidak dapat dibuat: " + parent);
		}
		if ("tmp".equalsIgnoreCase(parent.getName())) {
			cleanupStaleTempFiles(parent, 24L * 60L * 60L * 1000L);
		}
		long required = estimatedBytes > MINIMUM_FREE_BYTES ? estimatedBytes : MINIMUM_FREE_BYTES;
		long usable = parent.getUsableSpace();
		if (usable < required) {
			throw new IOException("Ruang penyimpanan server tidak mencukupi. Tersedia "
					+ (usable / (1024L * 1024L)) + " MB, diperlukan sedikitnya "
					+ (required / (1024L * 1024L)) + " MB.");
		}
	}

	/** Hapus hanya file biasa yang sudah kedaluwarsa dari direktori temporary.
	 * Tidak rekursif dan tidak pernah menyentuh subdirektori/template aplikasi. */
	public static void cleanupStaleTempFiles(File directory, long maximumAgeMillis) {
		if (directory == null || !directory.isDirectory()) return;
		File[] files = directory.listFiles();
		if (files == null) return;
		long cutoff = System.currentTimeMillis() - maximumAgeMillis;
		for (int i = 0; i < files.length; i++) {
			File candidate = files[i];
			if (candidate != null && candidate.isFile() && candidate.lastModified() < cutoff) {
				try { candidate.delete(); } catch (SecurityException ignored) { }
			}
		}
	}

	/**
	 * Menyalin data antar channel dengan buffer direct. Disederhanakan menggunakan
	 * pola flip/write-drain/clear yang efisien untuk blocking stream.
	 */
	public static void fastChannelCopy(ReadableByteChannel src, WritableByteChannel dest) throws IOException {
		final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);

		while (src.read(buffer) != -1) {
			// Siapkan buffer untuk dibaca (flip dari write-mode ke read-mode)
			buffer.flip();

			// Pastikan semua data dalam buffer tertulis ke tujuan
			while (buffer.hasRemaining()) {
				dest.write(buffer);
			}

			// Bersihkan buffer untuk pembacaan berikutnya (clear lebih cepat daripada
			// compact untuk kasus ini)
			buffer.clear();
		}
	}

	public static void writeInputStreamToFile(InputStream inputStream, File file) {
		if (inputStream == null || file == null) {
			return;
		}

		if (file.exists()) {
			return;
		}

		// Pastikan folder parent ada, hindari NPE jika file ada di root atau parent
		// null
		File parent = file.getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}

		// Try-with-resources (Java 7+) menjamin stream & channel ditutup otomatis
		try {
			ReadableByteChannel inputChannel = Channels.newChannel(inputStream);
			FileOutputStream fos = new FileOutputStream(file);
			WritableByteChannel outputChannel = fos.getChannel();
			fastChannelCopy(inputChannel, outputChannel);
			fos.close();
			inputChannel.close();
		} catch (IOException e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonFileUtil.java:68");
		}
	}

	public static void writeBlobToFile(Blob blob, File file) {
		if (blob == null || file == null) {
			return;
		}

		// FIX PSQLException "Large Objects may not be used in auto-commit mode":
		// PostgreSQL MEWAJIBKAN pembacaan Large Object (blob.getBinaryStream()) berjalan
		// di dalam transaksi eksplisit (non-autocommit). Semua caller method ini (lihat
		// CommonFileMediaHelper.writeBlobToFile -> Common.writeBlobToFile) mengambil Blob-nya
		// lewat StreamingHibernateUtil, dan beberapa memakai currentSession()/openSession()
		// TANPA beginTransaction() (mis. dipanggil dari dalam Timer callback di
		// UploadBiodataCalonMahasiswaSPANPTKINAction, atau query lepas di ItemTreeAction),
		// sehingga koneksinya masih dalam mode auto-commit saat stream-nya baru dibaca di
		// sini. Pola yang sama (bungkus pembacaan Blob dengan transaksi eksplisit) sudah
		// dipakai di CommonReport.handleUploadReport dan AmbilGaleriFotoImage.process.
		// Di sini kita jaga secara defensif: mulai transaksi HANYA bila belum ada transaksi
		// aktif di session streaming saat ini, dan HANYA commit transaksi yang KITA mulai
		// sendiri -- supaya transaksi caller yang sudah berjalan (mis. sudah di dalam
		// beginTransaction() sendiri) tidak ikut ter-commit prematur oleh helper ini. Sesi
		// HibernateUtil (non-streaming) sengaja TIDAK disentuh di sini karena tidak ada
		// caller writeBlobToFile yang mengambil Blob dari sana (lihat verifikasi caller).
		Transaction txStreaming = null;
		try {
			try {
				Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
				if (streamingSession != null && streamingSession.isOpen()) {
					Transaction existing = streamingSession.getTransaction();
					if (existing == null || !existing.isActive()) {
						txStreaming = streamingSession.beginTransaction();
					}
				}
			} catch (Exception eStream) { ais.common.ErrorAuditUtil.record(eStream, "auto-audit(empty-catch) src/ais/common/CommonFileUtil.java:writeBlobToFile-streaming"); }

			// Delegasikan ke method writeInputStreamToFile agar tidak ada duplikasi kode
			writeInputStreamToFile(blob.getBinaryStream(), file);
		} catch (SQLException e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (txStreaming != null) {
				try { txStreaming.commit(); } catch (Exception eCommit) { ais.common.ErrorAuditUtil.record(eCommit, "auto-audit(empty-catch) src/ais/common/CommonFileUtil.java:writeBlobToFile-commit-streaming"); }
			}
		}
	}

	public static File getCreateRandomFile() {
		// Menggunakan incrementAndGet() untuk operasi atomik yang aman
		String uniqueName = ais.ui.util.WaktuUtil.getDate().getTime() + "_" + increments.incrementAndGet();
		File myFile = new File("/opt/file_ais/" + uniqueName);

		File parent = myFile.getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}

		return myFile;
	}
}
