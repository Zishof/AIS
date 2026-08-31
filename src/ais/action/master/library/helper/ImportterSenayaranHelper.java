package ais.action.master.library.helper;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.library.Perpustakaan;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.io.StringWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.hibernate.SQLQuery;
import org.hibernate.Session;

/**
 * Helper migrasi data dari SLiMS/Senayan (aplikasi perpustakaan open-source berbasis MySQL) ke
 * modul perpustakaan AIS ({@code library.*}), dipicu lewat unggahan satu berkas ZIP ekspor
 * Senayan. Alur kerja ({@link #importZip}): (1) memvalidasi dan mengekstrak ZIP (harus berisi
 * tepat satu berkas {@code .sql} dan satu folder {@code images}); (2) menyalin gambar sampul ke
 * {@link #IMAGE_TARGET_ROOT}; (3) membaca dump SQL MySQL statement demi statement (menghormati
 * string berkutip agar titik koma di dalam string tidak dianggap pemisah statement),
 * mengonversi {@code CREATE TABLE}/{@code INSERT INTO} ke dialek PostgreSQL (lihat
 * {@link #convertCreateTable}/{@link #convertInsert}/{@link #mapMysqlType}) dan menjalankannya ke
 * sebuah <b>schema staging</b> sementara bernama {@code senayan_<timestamp>} — statement jenis
 * lain (DDL selain CREATE TABLE, transaksi, kunci tabel, dsb.) dilewati; (4) memigrasikan data
 * dari schema staging ke tabel-tabel {@code library.*} produksi ({@link #migrateToLibrary}).
 * Setiap tahap melaporkan progres lewat {@link ProgressListener} dan mencatat log rinci/diagnostik
 * ke {@link Result#reportFile} untuk membantu audit bila ada statement yang gagal direstore.
 * Kegagalan pada tahap mana pun dibungkus sebagai {@link ImportException} yang menyertakan lokasi
 * berkas laporan diagnostik.
 */
public class ImportterSenayaranHelper {

	/** Callback progres impor: dipanggil berkala dengan persentase (0-100) dan pesan status berjalan. */
	public interface ProgressListener {
		void onProgress(int percent, String message);
	}

	/** Kumpulan hasil dan statistik satu proses impor ZIP Senayan: lokasi berkas kerja, jumlah statement SQL yang berhasil/dilewati/gagal direstore, jumlah item/barcode/anggota yang berhasil dimigrasikan, serta log pesan proses. */
	public static class Result {
		public String schema;
		public File zipFile;
		public File extractDirectory;
		public File sqlFile;
		public File imageDirectory;
		public File migratedImageDirectory;
		public File reportFile;
		public int restoredStatements;
		public int skippedStatements;
		public int failedStatements;
		public int importedItems;
		public int importedBarcodes;
		public int importedMembers;
		public String currentStep;
		public String currentSql;
		public final List<String> messages = new ArrayList<String>();
	}

	/** Exception yang membungkus kegagalan proses impor, menyertakan lokasi {@link #getReportFile() berkas laporan diagnostik} yang sudah ditulis sejauh proses berjalan. */
	public static class ImportException extends Exception {
		private static final long serialVersionUID = 1L;
		private final File reportFile;

		/** Membungkus {@code cause} dengan {@code message} dan menyertakan {@code reportFile} untuk diagnosis lanjut. */
		public ImportException(String message, Throwable cause, File reportFile) {
			super(message, cause);
			this.reportFile = reportFile;
		}

		/** Berkas teks berisi log/diagnostik proses impor sejauh sempat berjalan sebelum gagal. */
		public File getReportFile() {
			return reportFile;
		}
	}

	private static final String IMAGE_TARGET_ROOT = "/opt/gambar_perpus";
	private Result activeResult;

	/**
	 * Menjalankan seluruh pipeline migrasi Senayan dari {@code zipFile} — lihat alur lengkap pada
	 * dokumentasi kelas. Direktori kerja sementara dibuat di bawah {@code java.io.tmpdir} dengan
	 * nama schema unik berbasis timestamp.
	 *
	 * @return {@link Result} berisi lokasi berkas kerja dan statistik lengkap proses migrasi
	 * @throws ImportException bila proses gagal pada tahap mana pun (validasi, ekstraksi, restore SQL, atau migrasi)
	 */
	public Result importZip(File zipFile, ProgressListener progressListener) throws Exception {
		Result result = new Result();
		File baseDir = new File(System.getProperty("java.io.tmpdir"), "senayan-import");
		if (!baseDir.exists()) {
			baseDir.mkdirs();
		}
		String stamp = new SimpleDateFormat("ddMMyyyy_HHmmss").format(new Date());
		result.schema = "senayan_" + stamp;
		result.zipFile = zipFile;
		result.extractDirectory = new File(baseDir, result.schema);
		result.reportFile = new File(baseDir, result.schema + "-diagnostic.txt");
		if (!result.extractDirectory.exists()) {
			result.extractDirectory.mkdirs();
		}
		try {
			activeResult = result;
			writeReportHeader(result);
			update(progressListener, 1, "Memvalidasi file ZIP Senayan.");
			log(result, "INFO", "Mulai import ZIP Senayan.");
			if (zipFile == null || !zipFile.exists() || !zipFile.getName().toLowerCase(Locale.ENGLISH).endsWith(".zip")) {
				throw new IllegalArgumentException("File yang di-upload harus berformat .zip.");
			}

			validateZip(zipFile, result);
			update(progressListener, 7, "Format ZIP valid. Mengekstrak file upload.");
			log(result, "OK", "Format ZIP valid. Extract ke " + result.extractDirectory.getAbsolutePath());
			extractZipSafely(zipFile, result.extractDirectory);
			result.sqlFile = findSqlFile(result.extractDirectory);
			result.imageDirectory = findImagesDirectory(result.extractDirectory);
			if (result.sqlFile == null || result.imageDirectory == null) {
				throw new IllegalArgumentException("Format ZIP tidak sesuai. ZIP harus berisi tepat 1 file .sql dan folder images.");
			}
			log(result, "OK", "File SQL ditemukan: " + result.sqlFile.getAbsolutePath());
			log(result, "OK", "Folder images ditemukan: " + result.imageDirectory.getAbsolutePath());

			update(progressListener, 14, "Menyalin gambar Senayan ke folder library.");
			result.migratedImageDirectory = new File(IMAGE_TARGET_ROOT, result.schema);
			copyDirectory(result.imageDirectory, result.migratedImageDirectory);
			log(result, "OK", "Images disalin ke " + result.migratedImageDirectory.getAbsolutePath());

			update(progressListener, 20, "Membuat schema staging " + result.schema + ".");
			restoreSql(result, progressListener);

			update(progressListener, 72, "Mulai migrasi staging Senayan ke tabel library.");
			migrateToLibrary(result, progressListener);

			update(progressListener, 100, "Migrasi Senayan selesai. Item: " + result.importedItems + ", barcode: "
					+ result.importedBarcodes + ", anggota: " + result.importedMembers + ".");
			logSummary(result, "SUKSES");
			return result;
		} catch (Exception e) {
			log(result, "ERROR", "Import gagal: " + shortMessage(e));
			logSummary(result, "GAGAL");
			throw new ImportException("Import Senayan gagal: " + e.getMessage(), e, result.reportFile);
		} finally {
			activeResult = null;
		}
	}

	/**
	 * Memvalidasi struktur {@code zipFile} sebelum diekstrak: menolak entri dengan path tidak aman
	 * (mengandung {@code ..} atau path absolut — proteksi zip-slip/path traversal), dan
	 * mensyaratkan isi ZIP berupa tepat satu berkas {@code .sql} dan satu folder {@code images}.
	 */
	private void validateZip(File zipFile, Result result) throws IOException {
		ZipFile zip = new ZipFile(zipFile);
		try {
			int sqlCount = 0;
			boolean hasImages = false;
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				String name = normalizeZipName(entry.getName());
				if (name.indexOf("..") >= 0 || name.startsWith("/") || name.startsWith("\\")) {
					throw new IllegalArgumentException("ZIP berisi path yang tidak aman: " + entry.getName());
				}
				if (!entry.isDirectory() && name.toLowerCase(Locale.ENGLISH).endsWith(".sql")) {
					sqlCount++;
				}
				if (name.toLowerCase(Locale.ENGLISH).indexOf("/images/") >= 0
						|| name.toLowerCase(Locale.ENGLISH).endsWith("/images")) {
					hasImages = true;
				}
			}
			if (sqlCount != 1 || !hasImages) {
				throw new IllegalArgumentException("Format ZIP tidak sesuai. Ditemukan file SQL: " + sqlCount
						+ ", folder images: " + (hasImages ? "ada" : "tidak ada") + ".");
			}
		} finally {
			zip.close();
		}
	}

	/** Mengekstrak {@code zipFile} ke {@code destination}, memverifikasi ulang (defense-in-depth) bahwa jalur kanonik tiap entri tetap berada di dalam {@code destination} sebelum ditulis — mencegah zip-slip/path traversal. */
	private void extractZipSafely(File zipFile, File destination) throws IOException {
		String destinationPath = destination.getCanonicalPath() + File.separator;
		ZipFile zip = new ZipFile(zipFile);
		try {
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				File target = new File(destination, entry.getName());
				String targetPath = target.getCanonicalPath();
				if (!targetPath.startsWith(destinationPath)) {
					throw new IOException("ZIP berisi path yang tidak aman: " + entry.getName());
				}
				if (entry.isDirectory()) {
					target.mkdirs();
				} else {
					target.getParentFile().mkdirs();
					InputStream in = zip.getInputStream(entry);
					OutputStream out = new FileOutputStream(target);
					try {
						IOUtils.copyLarge(in, out);
					} finally {
						IOUtils.closeQuietly(out);
						IOUtils.closeQuietly(in);
					}
				}
			}
		} finally {
			zip.close();
		}
	}

	/** Mencari satu-satunya berkas {@code .sql} di bawah {@code root} (rekursif); {@code null} bila jumlahnya bukan tepat satu. */
	private File findSqlFile(File root) {
		List<File> files = new ArrayList<File>();
		findFiles(root, files, ".sql");
		return files.size() == 1 ? files.get(0) : null;
	}

	/** Mencari folder bernama {@code images} di bawah {@code root} hasil ekstraksi ZIP Senayan. */
	private File findImagesDirectory(File root) {
		if (root == null || !root.exists()) {
			return null;
		}
		File[] children = root.listFiles();
		if (children == null) {
			return null;
		}
		for (int i = 0; i < children.length; i++) {
			File child = children[i];
			if (child.isDirectory() && "images".equalsIgnoreCase(child.getName())) {
				return child;
			}
			if (child.isDirectory()) {
				File found = findImagesDirectory(child);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	/** Mengumpulkan secara rekursif seluruh berkas di bawah {@code root} yang namanya berakhiran {@code suffix} ke dalam {@code files}. */
	private void findFiles(File root, List<File> files, String suffix) {
		File[] children = root.listFiles();
		if (children == null) {
			return;
		}
		for (int i = 0; i < children.length; i++) {
			if (children[i].isDirectory()) {
				findFiles(children[i], files, suffix);
			} else if (children[i].getName().toLowerCase(Locale.ENGLISH).endsWith(suffix)) {
				files.add(children[i]);
			}
		}
	}

	/**
	 * Membaca {@code result.sqlFile} (dump MySQL) byte demi byte, membagi menjadi statement demi
	 * statement berdasarkan titik koma di luar string berkutip (menghormati escape backslash agar
	 * titik koma/kutip di dalam string tidak salah dianggap pemisah), lalu memproses tiap
	 * statement lewat {@link #processMysqlStatement} ke schema staging {@code result.schema} yang
	 * baru dibuat. Progres dilaporkan berkala berdasarkan proporsi byte yang sudah dibaca.
	 */
	private void restoreSql(Result result, ProgressListener progressListener) throws Exception {
		Session session = HibernateUtil.currentSession();
		executeSql(session, "create schema if not exists " + qident(result.schema));
		executeSql(session, "set standard_conforming_strings = off");
		log(result, "INFO", "Schema staging dibuat: " + result.schema);

		long length = result.sqlFile.length();
		long read = 0L;
		StringBuilder statement = new StringBuilder();
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(result.sqlFile));
		boolean inString = false;
		boolean escaped = false;
		int b;
		try {
			while ((b = in.read()) != -1) {
				read++;
				char c = (char) b;
				statement.append(c);
				if (inString) {
					if (escaped) {
						escaped = false;
					} else if (c == '\\') {
						escaped = true;
					} else if (c == '\'') {
						inString = false;
					}
				} else if (c == '\'') {
					inString = true;
				} else if (c == ';') {
					processMysqlStatement(session, statement.toString(), result);
					statement.setLength(0);
					if (result.restoredStatements % 100 == 0) {
						int percent = 20 + (int) Math.min(45L, (read * 45L) / Math.max(1L, length));
						update(progressListener, percent, "Restore SQL staging: " + result.restoredStatements
								+ " statement diproses, gagal " + result.failedStatements + ".");
					}
				}
			}
			if (statement.length() > 0) {
				processMysqlStatement(session, statement.toString(), result);
			}
		} finally {
			IOUtils.closeQuietly(in);
		}
		update(progressListener, 68, "Restore SQL selesai. Berhasil " + result.restoredStatements + ", dilewati "
				+ result.skippedStatements + ", gagal " + result.failedStatements + ".");
		log(result, "INFO", "Restore SQL selesai. Berhasil=" + result.restoredStatements + ", dilewati="
				+ result.skippedStatements + ", gagal=" + result.failedStatements);
	}

	/**
	 * Memproses satu statement SQL MySQL mentah: melewati (menambah {@code skippedStatements})
	 * statement kosong, komentar, atau jenis yang tidak relevan bagi staging (SET, transaksi,
	 * lock tabel, DROP/ALTER TABLE); mengonversi {@code CREATE TABLE}/{@code INSERT INTO} ke
	 * dialek PostgreSQL lalu menjalankannya, mencatat sukses/gagal (dengan diagnostik) ke
	 * {@code result}.
	 */
	private void processMysqlStatement(Session session, String raw, Result result) {
		String statement = stripLeadingMysqlComments(raw == null ? "" : raw.trim());
		if (statement.length() == 0 || statement.startsWith("--") || statement.startsWith("/*!")
				|| statement.toUpperCase(Locale.ENGLISH).startsWith("SET ")
				|| statement.toUpperCase(Locale.ENGLISH).startsWith("START TRANSACTION")
				|| statement.toUpperCase(Locale.ENGLISH).startsWith("COMMIT")
				|| statement.toUpperCase(Locale.ENGLISH).startsWith("LOCK TABLES")
				|| statement.toUpperCase(Locale.ENGLISH).startsWith("UNLOCK TABLES")
				|| statement.toUpperCase(Locale.ENGLISH).startsWith("DROP TABLE")
				|| statement.toUpperCase(Locale.ENGLISH).startsWith("ALTER TABLE")) {
			result.skippedStatements++;
			return;
		}
		String converted = null;
		if (statement.toUpperCase(Locale.ENGLISH).startsWith("CREATE TABLE")) {
			converted = convertCreateTable(statement, result.schema);
		} else if (statement.toUpperCase(Locale.ENGLISH).startsWith("INSERT INTO")) {
			converted = convertInsert(statement, result.schema);
		} else {
			result.skippedStatements++;
			return;
		}
		if (converted == null || converted.trim().length() == 0) {
			result.skippedStatements++;
			return;
		}
		try {
			executeSql(session, converted);
			result.restoredStatements++;
		} catch (Exception e) {
			result.failedStatements++;
			addDiagnostic(result, "Gagal restore " + statementKind(statement) + " pada tabel " + safeTableName(statement)
					+ ": " + shortMessage(e), statement);
		}
	}

	/** Menghapus baris komentar ({@code --} atau {@code #}) di awal {@code statement} secara berulang hingga tidak tersisa lagi. */
	private String stripLeadingMysqlComments(String statement) {
		String result = statement == null ? "" : statement.trim();
		boolean changed = true;
		while (changed) {
			changed = false;
			if (result.startsWith("--")) {
				int next = result.indexOf('\n');
				result = next < 0 ? "" : result.substring(next + 1).trim();
				changed = true;
			} else if (result.startsWith("#")) {
				int next = result.indexOf('\n');
				result = next < 0 ? "" : result.substring(next + 1).trim();
				changed = true;
			}
		}
		return result;
	}

	/** Mengonversi satu statement {@code CREATE TABLE} MySQL menjadi {@code CREATE TABLE IF NOT EXISTS} PostgreSQL pada {@code schema} staging, memetakan tiap definisi kolom lewat {@link #mapMysqlType}; mengembalikan {@code null} bila nama tabel atau daftar kolom tidak dapat diekstrak. */
	private String convertCreateTable(String statement, String schema) {
		String table = extractMysqlTableName(statement);
		if (table == null) {
			return null;
		}
		int open = statement.indexOf('(');
		int close = statement.lastIndexOf(')');
		if (open < 0 || close < open) {
			return null;
		}
		String body = statement.substring(open + 1, close);
		List<String> columns = splitTopLevel(body);
		StringBuilder sql = new StringBuilder();
		sql.append("create table if not exists ").append(qident(schema)).append(".").append(qident(table)).append(" (");
		boolean first = true;
		for (int i = 0; i < columns.size(); i++) {
			String col = columns.get(i).trim();
			if (!col.startsWith("`")) {
				continue;
			}
			int end = col.indexOf('`', 1);
			if (end < 0) {
				continue;
			}
			String name = col.substring(1, end);
			String rest = col.substring(end + 1).trim();
			if (!first) {
				sql.append(", ");
			}
			sql.append(qident(name)).append(" ").append(mapMysqlType(rest));
			first = false;
		}
		sql.append(")");
		return first ? null : sql.toString();
	}

	/** Mengonversi satu statement {@code INSERT INTO} MySQL agar menyasar {@code schema} staging: mengganti backtick dengan kutip ganda, melepas penanda {@code _binary}, membetulkan escape kutip tunggal, dan mengubah tanggal MySQL nol ({@code 0000-00-00...}) atau tanggal tidak valid menjadi {@code NULL} (PostgreSQL tidak menerima tanggal nol). */
	private String convertInsert(String statement, String schema) {
		String table = extractMysqlTableName(statement);
		if (table == null) {
			return null;
		}
		String sql = statement.replaceFirst("(?is)INSERT\\s+INTO\\s+`?" + table + "`?",
				"INSERT INTO " + qident(schema) + "." + qident(table));
		sql = sql.replace('`', '"');
		sql = sql.replaceAll("(?i)_binary\\s+'", "'");
		sql = sql.replace("\\'", "''");
		sql = sql.replaceAll("'0000-00-00 00:00:00'", "NULL");
		sql = sql.replaceAll("'0000-00-00'", "NULL");
		sql = sql.replaceAll("'[0-9]{4}-[0-9]{2}-[0-9]{2}[^']*\\?[^']*'", "NULL");
		return sql;
	}

	/** Mengekstrak nama tabel dari statement {@code CREATE TABLE}/{@code INSERT INTO} mentah, dicoba lewat identifier berkutip backtick lebih dulu, lalu jatuh kembali ke posisi kata ketiga bila tidak ada backtick. */
	private String extractMysqlTableName(String statement) {
		int tick = statement.indexOf('`');
		if (tick >= 0) {
			int end = statement.indexOf('`', tick + 1);
			return end > tick ? statement.substring(tick + 1, end) : null;
		}
		String[] parts = statement.split("\\s+");
		return parts.length >= 3 ? parts[2] : null;
	}

	/** Memetakan satu definisi kolom MySQL mentah (tipe data beserta atribut seperti {@code UNSIGNED}/{@code CHARACTER SET}/{@code DEFAULT}/{@code COMMENT}, yang dilepas karena tidak relevan di PostgreSQL) ke tipe data PostgreSQL yang paling mendekati (bigint/integer/smallint/timestamp/date/numeric/varchar/char/dst.). */
	private String mapMysqlType(String raw) {
		String t = raw.toLowerCase(Locale.ENGLISH);
		t = t.replaceAll("(?i)character\\s+set\\s+\\S+", " ");
		t = t.replaceAll("(?i)collate\\s+\\S+", " ");
		t = t.replaceAll("(?i)unsigned", " ");
		t = t.replaceAll("(?i)zerofill", " ");
		t = t.replaceAll("(?i)not\\s+null", " ").replaceAll("(?i)null", " ").replaceAll("(?i)auto_increment", " ")
				.replaceAll("(?i)on\\s+update\\s+current_timestamp", " ")
				.replaceAll("(?i)comment\\s+'([^'\\\\]|\\\\.)*'", " ")
				.replaceAll("(?i)default\\s+current_timestamp", " ")
				.replaceAll("(?i)default\\s+'0000-00-00( 00:00:00)?'", " ")
				.replaceAll("(?i)default\\s+[^\\s,]+", " ").trim();
		if (t.startsWith("bigint")) {
			return "bigint";
		}
		if (t.startsWith("int") || t.startsWith("mediumint")) {
			return "integer";
		}
		if (t.startsWith("smallint") || t.startsWith("tinyint")) {
			return "smallint";
		}
		if (t.startsWith("datetime") || t.startsWith("timestamp")) {
			return "timestamp";
		}
		if (t.startsWith("date")) {
			return "date";
		}
		if (t.startsWith("decimal") || t.startsWith("double") || t.startsWith("float")) {
			return "numeric";
		}
		if (t.startsWith("varchar")) {
			int end = t.indexOf(')');
			return end > 0 ? t.substring(0, end + 1) : "varchar(255)";
		}
		if (t.startsWith("char")) {
			int end = t.indexOf(')');
			return end > 0 ? t.substring(0, end + 1) : "char(1)";
		}
		if (t.startsWith("blob") || t.startsWith("longblob") || t.startsWith("mediumblob")) {
			return "bytea";
		}
		return "text";
	}

	/** Memecah {@code body} definisi kolom {@code CREATE TABLE} berdasarkan koma tingkat-atas (tidak memecah koma yang berada di dalam tanda kurung, mis. {@code decimal(10,2)}). */
	private List<String> splitTopLevel(String body) {
		List<String> result = new ArrayList<String>();
		StringBuilder current = new StringBuilder();
		int level = 0;
		for (int i = 0; i < body.length(); i++) {
			char c = body.charAt(i);
			if (c == '(') {
				level++;
			} else if (c == ')') {
				level--;
			} else if (c == ',' && level == 0) {
				result.add(current.toString());
				current.setLength(0);
				continue;
			}
			current.append(c);
		}
		if (current.length() > 0) {
			result.add(current.toString());
		}
		return result;
	}

	/**
	 * Memigrasikan data dari schema staging {@code result.schema} ke tabel-tabel produksi
	 * {@code library.*}, dalam tahapan (dilaporkan lewat {@code progressListener}): (1) memastikan
	 * baris {@code saldo_awal} (stok awal) dan kompatibilitas kolom staging tersedia
	 * ({@link #ensureSaldoAwal}/{@link #ensureStagingCompatibility}); (2) master penerbit,
	 * pengarang, dan kategori (upsert berdasarkan nama, tidak menduplikasi yang sudah ada); (3)
	 * bibliografi buku ({@code biblio} &rarr; {@code library.item}, termasuk path gambar sampul
	 * yang sudah dipindahkan ke {@link #IMAGE_TARGET_ROOT}) dan pembaruan baris yang sudah ada
	 * sebelumnya (deteksi via kolom {@code deep}=id biblio asal); (4) relasi item-pengarang dan
	 * item-kategori; (5) barcode item dan detail saldo awal; (6) anggota perpustakaan. Seluruh
	 * langkah SQL bersifat idempoten (memeriksa keberadaan terlebih dahulu) sehingga migrasi dapat
	 * dijalankan ulang dengan aman. Jumlah item/barcode/anggota yang berhasil dimigrasikan dicatat
	 * ke {@code result} di akhir.
	 */
	private void migrateToLibrary(Result result, ProgressListener progressListener) throws Exception {
		Session session = HibernateUtil.currentSession();
		try {
			result.currentStep = "Mencari user dan perpustakaan aktif";
			String userId = currentUserId(session);
			Long perpustakaanId = currentPerpustakaanId(session);
			if (perpustakaanId == null) {
				throw new IllegalStateException("Perpustakaan aktif tidak ditemukan.");
			}
			result.currentStep = "Membuat saldo awal dan tabel kompatibilitas staging";
			Long saldoAwalId = ensureSaldoAwal(session, perpustakaanId, userId, result.schema);
			ensureStagingCompatibility(session, result.schema);

		update(progressListener, 74, "Migrasi master penerbit, pengarang, dan kategori.");
		if (tableExists(session, result.schema, "mst_publisher")) {
			executeSql(session, "insert into library.penerbit(nama, tanggal_dirubah) "
					+ "select distinct trim(publisher_name), now() from " + qident(result.schema)
					+ ".mst_publisher s where nullif(trim(publisher_name),'') is not null "
					+ "and not exists (select 1 from library.penerbit p where lower(trim(p.nama))=lower(trim(s.publisher_name)))");
		}
		if (tableExists(session, result.schema, "mst_author")) {
			executeSql(session, "insert into library.pengarang(kode, nama, aktif, tanggal_dirubah) "
					+ "select cast(author_id as text), trim(author_name), true, now() from " + qident(result.schema)
					+ ".mst_author s where nullif(trim(author_name),'') is not null "
					+ "and not exists (select 1 from library.pengarang p where lower(trim(p.nama))=lower(trim(s.author_name)))");
		}
		if (tableExists(session, result.schema, "mst_topic")) {
			executeSql(session, "insert into library.kategori_item(kode, nama, tanggal_dirubah) "
					+ "select cast(topic_id as text), upper(trim(topic)), now() from " + qident(result.schema)
					+ ".mst_topic s where nullif(trim(topic),'') is not null "
					+ "and not exists (select 1 from library.kategori_item k where lower(trim(k.nama))=lower(trim(s.topic)))");
		}

		update(progressListener, 80, "Migrasi bibliografi dan cover buku.");
		if (tableExists(session, result.schema, "biblio")) {
			String imageRoot = result.migratedImageDirectory.getAbsolutePath().replace("\\", "/");
			executeSql(session, "update library.item i set "
					+ "kode=cast(b.biblio_id as text), nama=b.title, pengarangs=b.sor, edisi=b.edition, "
					+ "isbn=replace(coalesce(b.isbn_issn,''),'-',''), tahun=case when b.publish_year ~ '^[0-9]{1,4}$' then cast(b.publish_year as integer) else 0 end, "
					+ "penaklikan=b.collation, callnumber=b.call_number, bahasa=coalesce(l.language_name,b.language_id), "
					+ "tempatterbit=pl.place_name, dewey_decimal_class=b.classification, deweydecimalclass=b.classification, "
					+ "imagepath=case when nullif(trim(b.image),'') is null then i.imagepath else '" + esc(imageRoot)
					+ "/' || b.image end, catatan=coalesce(nullif(b.spec_detail_info,''), b.notes), tanggal_dirubah=now() "
					+ "from " + qident(result.schema) + ".biblio b "
					+ "left join " + qident(result.schema) + ".mst_language l on l.language_id=b.language_id "
					+ "left join " + qident(result.schema) + ".mst_place pl on pl.place_id=b.publish_place_id "
					+ "where i.deep=b.biblio_id");
			executeSql(session, "insert into library.item(kode, deep, nama, pengarangs, edisi, isbn, penerbit, tahun, "
					+ "penaklikan, callnumber, bahasa, tempatterbit, dewey_decimal_class, deweydecimalclass, imagepath, "
					+ "catatan, tanggal, tanggalterbit, folder, halaman, urutan, tanggal_dirubah, jenis_item, tipe_item) "
					+ "select cast(b.biblio_id as text), b.biblio_id, b.title, b.sor, b.edition, replace(coalesce(b.isbn_issn,''),'-',''), "
					+ "(select p.id from library.penerbit p where lower(trim(p.nama))=lower(trim(mp.publisher_name)) limit 1), "
					+ "case when b.publish_year ~ '^[0-9]{1,4}$' then cast(b.publish_year as integer) else 0 end, b.collation, b.call_number, "
					+ "coalesce(l.language_name,b.language_id), pl.place_name, b.classification, b.classification, "
					+ "case when nullif(trim(b.image),'') is null then null else '" + esc(imageRoot) + "/' || b.image end, "
					+ "coalesce(nullif(b.spec_detail_info,''), b.notes), coalesce(b.input_date, now()), coalesce(b.input_date, now()), "
					+ "false, 0, 0, now(), "
					+ "coalesce((select ji.id from library.jenis_item ji where ji.id=b.gmd_id limit 1),(select ji.id from library.jenis_item ji order by ji.id limit 1)), "
					+ "(select ti.id from library.tipe_item ti order by ti.id limit 1) "
					+ "from " + qident(result.schema) + ".biblio b "
					+ "left join " + qident(result.schema) + ".mst_publisher mp on mp.publisher_id=b.publisher_id "
					+ "left join " + qident(result.schema) + ".mst_language l on l.language_id=b.language_id "
					+ "left join " + qident(result.schema) + ".mst_place pl on pl.place_id=b.publish_place_id "
					+ "where not exists (select 1 from library.item i where i.deep=b.biblio_id)");
			result.importedItems = count(session, "select count(*) from library.item i where i.deep in (select biblio_id from "
					+ qident(result.schema) + ".biblio)");
		}

		update(progressListener, 86, "Migrasi relasi pengarang dan kategori item.");
		if (tableExists(session, result.schema, "biblio_author") && tableExists(session, result.schema, "mst_author")) {
			executeSql(session, "insert into library.item_punya_pengarang(item, pengarang) "
					+ "select distinct i.id, p.id from " + qident(result.schema) + ".biblio_author ba "
					+ "join " + qident(result.schema) + ".mst_author ma on ma.author_id=ba.author_id "
					+ "join library.item i on i.deep=ba.biblio_id "
					+ "join library.pengarang p on lower(trim(p.nama))=lower(trim(ma.author_name)) "
					+ "where not exists (select 1 from library.item_punya_pengarang x where x.item=i.id and x.pengarang=p.id)");
		}
		if (tableExists(session, result.schema, "biblio_topic") && tableExists(session, result.schema, "mst_topic")) {
			executeSql(session, "insert into library.item_punya_kategori_item(item, kategori_item) "
					+ "select distinct i.id, k.id from " + qident(result.schema) + ".biblio_topic bt "
					+ "join " + qident(result.schema) + ".mst_topic mt on mt.topic_id=bt.topic_id "
					+ "join library.item i on i.deep=bt.biblio_id "
					+ "join library.kategori_item k on lower(trim(k.nama))=lower(trim(mt.topic)) "
					+ "where not exists (select 1 from library.item_punya_kategori_item x where x.item=i.id and x.kategori_item=k.id)");
			executeSql(session, "update library.item i set kategories=x.nama from (select ipk.item, string_agg(k.nama, ', ') nama "
					+ "from library.item_punya_kategori_item ipk join library.kategori_item k on k.id=ipk.kategori_item group by ipk.item) x "
					+ "where x.item=i.id and i.deep in (select biblio_id from " + qident(result.schema) + ".biblio)");
		}

		update(progressListener, 91, "Migrasi barcode dan saldo awal.");
		if (tableExists(session, result.schema, "item")) {
			executeSql(session, "insert into library.saldo_awal_detail(item, saldo_awal, jumlah, data_per_item, tanggal_dirubah) "
					+ "select i.id, " + saldoAwalId + ", 0, false, now() from library.item i "
					+ "join " + qident(result.schema) + ".biblio b on b.biblio_id=i.deep "
					+ "where not exists (select 1 from library.saldo_awal_detail s where s.saldo_awal=" + saldoAwalId
					+ " and s.item=i.id)");
			executeSql(session, "insert into library.batch_item_punya_barcode(perpustakaan, saldo_awal, item, tanggal, tanggal_dirubah) "
					+ "select " + perpustakaanId + ", " + saldoAwalId + ", i.id, now(), now() from library.item i "
					+ "join " + qident(result.schema) + ".biblio b on b.biblio_id=i.deep "
					+ "where not exists (select 1 from library.batch_item_punya_barcode bb where bb.saldo_awal=" + saldoAwalId
					+ " and bb.item=i.id and bb.perpustakaan=" + perpustakaanId + ")");
			executeSql(session, "insert into library.item_punya_barcode(item, perpustakaan, batch_item_punya_barcode, barcode, indexke, tipe_item, tanggal_dirubah) "
					+ "select i.id, " + perpustakaanId + ", bb.id, s.item_code, row_number() over(partition by i.id order by s.item_id)-1, i.tipe_item, now() "
					+ "from " + qident(result.schema) + ".item s join library.item i on i.deep=s.biblio_id "
					+ "join library.batch_item_punya_barcode bb on bb.item=i.id and bb.saldo_awal=" + saldoAwalId
					+ " and bb.perpustakaan=" + perpustakaanId + " where nullif(trim(s.item_code),'') is not null "
					+ "and not exists (select 1 from library.item_punya_barcode ipb where lower(ipb.barcode)=lower(s.item_code))");
			result.importedBarcodes = count(session, "select count(*) from library.item_punya_barcode ipb join library.item i on i.id=ipb.item "
					+ "where i.deep in (select biblio_id from " + qident(result.schema) + ".biblio)");
			executeSql(session, "update library.saldo_awal_detail sad set jumlah=x.jumlah, batch_item_punya_barcode=x.batch_id "
					+ "from (select ipb.item, cast(count(*) as float8) jumlah, max(ipb.batch_item_punya_barcode) batch_id from library.item_punya_barcode ipb "
					+ "where ipb.perpustakaan=" + perpustakaanId + " group by ipb.item) x where sad.item=x.item and sad.saldo_awal="
					+ saldoAwalId);
		}

		update(progressListener, 96, "Migrasi anggota Senayan.");
		if (tableExists(session, result.schema, "member")) {
			executeSql(session, "insert into library.anggota(perpustakaan, aktif, alamat, email_anggota, hp, telp, jenis_identitas_anggota, kode, kode_identitas, nama, tanggal_dirubah) "
					+ "select " + perpustakaanId + ", true, member_address, member_email, member_phone, member_phone, "
					+ "(select j.id from library.jenis_identitas_anggota j where lower(trim(j.nama))='ktp' order by j.id limit 1), "
					+ "member_id, member_id, member_name, now() "
					+ "from " + qident(result.schema) + ".member m where nullif(trim(member_id),'') is not null "
					+ "and not exists (select 1 from library.anggota a where a.kode=m.member_id)");
			result.importedMembers = count(session, "select count(*) from library.anggota a where exists (select 1 from "
					+ qident(result.schema) + ".member m where m.member_id=a.kode)");
		}
			log(result, "INFO", "Migrasi library selesai. Item=" + result.importedItems + ", barcode="
					+ result.importedBarcodes + ", anggota=" + result.importedMembers);
		} catch (Exception e) {
			log(result, "ERROR", "Migrasi gagal pada fase: " + (result.currentStep == null ? "-" : result.currentStep));
			if (result.currentSql != null) {
				log(result, "SQL", abbreviate(result.currentSql, 8000));
			}
			logException(result, e);
			throw e;
		}
	}

	/** Membuat (atau mengambil bila sudah ada) satu baris {@code library.saldo_awal} (stok awal) untuk import Senayan ini, dikaitkan ke {@code perpustakaanId} dan {@code userId} pembuat, ditandai dengan {@code schema} staging untuk idempotensi. */
	private Long ensureSaldoAwal(Session session, Long perpustakaanId, String userId, String schema) {
		if (userId == null) {
			userId = "admin";
		}
		String kode = "SALDO_AWAL_SENAYAN_" + schema;
		Long id = numberToLong((Number) session
				.createSQLQuery("select id from library.saldo_awal where kode=:kode").setParameter("kode", kode)
				.setMaxResults(1).uniqueResult());
		if (id != null) {
			return id;
		}
		org.hibernate.Transaction tx = null;
		try {
			if (!session.getTransaction().isActive()) {
				tx = session.beginTransaction();
			}
			SQLQuery query = session.createSQLQuery("insert into library.saldo_awal(kode, keterangan, perpustakaan, "
					+ "tanggal_pembuatan, tanggal_persetujuan, dibuat_oleh, disetujui_oleh, tanggal_dirubah) values "
					+ "(:kode, :ket, :perpus, now(), now(), :userId, :userId, now())");
			query.setParameter("kode", kode);
			query.setParameter("ket", "Saldo awal otomatis dari import ZIP Senayan schema " + schema);
			query.setParameter("perpus", perpustakaanId);
			query.setParameter("userId", userId);
			query.executeUpdate();
			if (tx != null) {
				tx.commit();
			}
		} catch (RuntimeException e) {
			if (tx != null && tx.isActive()) {
				tx.rollback();
			}
			throw e;
		}
		id = numberToLong((Number) session.createSQLQuery("select id from library.saldo_awal where kode=:kode")
				.setParameter("kode", kode).setMaxResults(1).uniqueResult());
		if (id == null) {
			throw new IllegalStateException("Saldo awal import Senayan tidak dapat dibuat untuk kode " + kode);
		}
		return id;
	}

	/** Membuat tabel staging kosong (mst_publisher/mst_language/mst_place/item) bila belum ada pada dump Senayan, agar query migrasi berikutnya yang mereferensikannya tidak gagal. */
	private void ensureStagingCompatibility(Session session, String schema) {
		executeSql(session, "create table if not exists " + qident(schema)
				+ ".mst_publisher(publisher_id integer, publisher_name text)");
		executeSql(session, "create table if not exists " + qident(schema)
				+ ".mst_language(language_id text, language_name text)");
		executeSql(session, "create table if not exists " + qident(schema)
				+ ".mst_place(place_id integer, place_name text)");
		executeSql(session, "create table if not exists " + qident(schema)
				+ ".item(item_id integer, biblio_id integer, item_code text, coll_type_id integer)");
	}

	/** Mengambil id user yang sedang login (untuk dicatat sebagai pembuat data hasil migrasi), {@code null} bila tidak tersedia. */
	private String currentUserId(Session session) {
		try {
			Tbmuser user = Common.getCurrentUser();
			if (user != null && user.getUserId() != null && user.getUserId().trim().length() > 0) {
				return user.getUserId();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/helper/ImportterSenayaranHelper.java:668");
		}
		String id = (String) session.createSQLQuery("select userid from public.tbmuser order by userid limit 1")
				.uniqueResult();
		return id == null ? "admin" : id;
	}

	/** Mengambil id {@link Perpustakaan} tujuan migrasi dari user yang sedang login ({@link Common#getCurrentPerpustakaan()}), atau bila tidak tersedia, id perpustakaan pertama yang ada di database sebagai fallback. */
	private Long currentPerpustakaanId(Session session) {
		try {
			Perpustakaan perpustakaan = Common.getCurrentPerpustakaan();
			if (perpustakaan != null && perpustakaan.getId() != null) {
				return perpustakaan.getId();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/helper/ImportterSenayaranHelper.java:681");
		}
		return numberToLong((Number) session.createSQLQuery("select id from library.perpustakaan order by id limit 1")
				.uniqueResult());
	}

	/** Memeriksa keberadaan {@code table} pada {@code schema} lewat {@code information_schema.tables} — dipakai untuk membuat setiap langkah migrasi tahan terhadap dump Senayan yang tidak lengkap. */
	private boolean tableExists(Session session, String schema, String table) {
		Number n = (Number) session
				.createSQLQuery("select count(*) from information_schema.tables where table_schema=:schema and table_name=:table")
				.setParameter("schema", schema).setParameter("table", table).uniqueResult();
		return n != null && n.intValue() > 0;
	}

	/** Menjalankan {@code sql} berupa {@code SELECT COUNT(*)...} dan mengembalikan hasilnya sebagai {@code int}; mencatat {@code sql} ke {@link #activeResult} untuk keperluan diagnostik bila terjadi kegagalan. */
	private int count(Session session, String sql) {
		if (activeResult != null) {
			activeResult.currentSql = sql;
		}
		Number n = (Number) session.createSQLQuery(sql).uniqueResult();
		return n == null ? 0 : n.intValue();
	}

	/** Menjalankan {@code sql} native (DDL/DML) dalam transaksi sendiri (dibuat bila belum ada transaksi aktif, rollback otomatis bila gagal); mencatat {@code sql} ke {@link #activeResult} untuk diagnostik. */
	private void executeSql(Session session, String sql) {
		if (activeResult != null) {
			activeResult.currentSql = sql;
		}
		org.hibernate.Transaction tx = null;
		try {
			if (!session.getTransaction().isActive()) {
				tx = session.beginTransaction();
			}
			session.createSQLQuery(sql).executeUpdate();
			if (tx != null) {
				tx.commit();
			}
		} catch (RuntimeException e) {
			if (tx != null && tx.isActive()) {
				tx.rollback();
			}
			throw e;
		}
	}

	/** Menyalin {@code source} (berkas atau direktori, rekursif) ke {@code target}, membuat direktori tujuan sesuai kebutuhan. */
	private static void copyDirectory(File source, File target) throws IOException {
		if (source == null || !source.exists()) {
			return;
		}
		if (source.isDirectory()) {
			if (!target.exists()) {
				target.mkdirs();
			}
			File[] children = source.listFiles();
			if (children != null) {
				for (int i = 0; i < children.length; i++) {
					copyDirectory(children[i], new File(target, children[i].getName()));
				}
			}
		} else {
			target.getParentFile().mkdirs();
			InputStream in = new FileInputStream(source);
			OutputStream out = new FileOutputStream(target);
			try {
				IOUtils.copyLarge(in, out);
			} finally {
				IOUtils.closeQuietly(out);
				IOUtils.closeQuietly(in);
			}
		}
	}

	/** Menormalkan pemisah path entri ZIP ke {@code /}, mengantisipasi ZIP yang dibuat di Windows. */
	private static String normalizeZipName(String name) {
		return name == null ? "" : name.replace('\\', '/');
	}

	/** Meng-quote {@code name} sebagai identifier SQL PostgreSQL (tanda kutip ganda, dengan escaping kutip ganda internal). */
	private static String qident(String name) {
		return "\"" + name.replace("\"", "\"\"") + "\"";
	}

	/** Meng-escape kutip tunggal pada {@code s} untuk disisipkan aman sebagai literal string SQL. */
	private static String esc(String s) {
		return s == null ? "" : s.replace("'", "''");
	}

	/** Mengonversi {@code n} menjadi {@link Long}, {@code null} bila {@code n} {@code null}. */
	private static Long numberToLong(Number n) {
		return n == null ? null : Long.valueOf(n.longValue());
	}

	/** Menyusun pesan galat ringkas (maks. 240 karakter) dari akar penyebab {@code e}, untuk ditampilkan pada log/diagnostik. */
	private static String shortMessage(Exception e) {
		String message = rootCauseMessage(e);
		if (message == null) {
			message = e.getClass().getName();
		}
		return message.length() > 240 ? message.substring(0, 240) : message;
	}

	/** Menelusuri rantai {@code getCause()} hingga akar, mengembalikan pesannya (atau pesan {@code e} sendiri bila akar tidak punya pesan, atau nama kelas exception sebagai upaya terakhir). */
	private static String rootCauseMessage(Throwable e) {
		if (e == null) {
			return "";
		}
		Throwable root = e;
		while (root.getCause() != null && root.getCause() != root) {
			root = root.getCause();
		}
		String message = root.getMessage();
		if (message == null || message.trim().length() == 0) {
			message = e.getMessage();
		}
		return message == null ? root.getClass().getName() : message;
	}

	/** Menulis (menimpa) header awal {@code result.reportFile}: waktu mulai, schema staging, lokasi ZIP, dan catatan bahwa berkas ini dapat ditempel ke AI untuk analisis kegagalan. */
	private void writeReportHeader(Result result) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(result.reportFile, false));
			writer.write("DIAGNOSTIC IMPORT SENAYAN");
			writer.newLine();
			writer.write("Waktu        : " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
			writer.newLine();
			writer.write("Status       : BERJALAN");
			writer.newLine();
			writer.write("Schema       : " + result.schema);
			writer.newLine();
			writer.write("ZIP          : " + (result.zipFile == null ? "-" : result.zipFile.getAbsolutePath()));
			writer.newLine();
			writer.write("Catatan      : File ini dapat di-copy ke AI untuk analisis error import Senayan.");
			writer.newLine();
			writer.write("============================================================");
			writer.newLine();
		} catch (Exception e) {
			System.out.println("Gagal membuat diagnostic import Senayan: " + shortMessage(e));
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	/** Mencatat ringkasan akhir proses (statistik statement/item/barcode/anggota) ke log, lalu menambahkan ke {@code reportFile} sebuah "prompt" siap-pakai yang menuntun AI menganalisis kegagalan migrasi MySQL-ke-PostgreSQL (nilai tanggal nol, escape, tipe data, dsb.) bila diperlukan. */
	private void logSummary(Result result, String status) {
		log(result, "SUMMARY", "Status=" + status + ", schema=" + result.schema + ", restored="
				+ result.restoredStatements + ", skipped=" + result.skippedStatements + ", failed="
				+ result.failedStatements + ", item=" + result.importedItems + ", barcode="
				+ result.importedBarcodes + ", anggota=" + result.importedMembers);
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(result.reportFile, true));
			writer.newLine();
			writer.write("PROMPT UNTUK AI");
			writer.newLine();
			writer.write("Tolong analisis error import database Senayan/MySQL ke PostgreSQL berikut. ");
			writer.write("Jelaskan penyebab, statement/tabel yang bermasalah, dan patch Java/SQL yang perlu dibuat. ");
			writer.write("Perhatikan nilai tanggal nol MySQL, escape apostrof/backslash, tipe data MySQL, dan kompatibilitas PostgreSQL.");
			writer.newLine();
			writer.write("File ZIP: " + (result.zipFile == null ? "-" : result.zipFile.getAbsolutePath()));
			writer.newLine();
			writer.write("File SQL: " + (result.sqlFile == null ? "-" : result.sqlFile.getAbsolutePath()));
			writer.newLine();
			writer.write("Schema staging: " + result.schema);
			writer.newLine();
			writer.write("Status akhir: " + status);
			writer.newLine();
			writer.write("============================================================");
			writer.newLine();
		} catch (Exception e) {
			System.out.println("Gagal menulis summary diagnostic import Senayan: " + shortMessage(e));
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	/** Menambahkan satu baris log berstempel waktu (jam:menit:detik) dan {@code level} ke {@code result.reportFile}. */
	private void log(Result result, String level, String message) {
		if (result == null || result.reportFile == null) {
			return;
		}
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(result.reportFile, true));
			writer.write(new SimpleDateFormat("HH:mm:ss").format(new Date()));
			writer.write(" [" + level + "] ");
			writer.write(message == null ? "" : message);
			writer.newLine();
		} catch (Exception e) {
			System.out.println("Gagal menulis diagnostic import Senayan: " + shortMessage(e));
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	/** Mencatat rincian lengkap {@code e} ke laporan: akar penyebab, seluruh rantai {@code cause}, detail {@link SQLException} (SQL state, kode error, exception berantai berikutnya), dan stack trace penuh. */
	private void logException(Result result, Exception e) {
		log(result, "ROOT_CAUSE", rootCauseMessage(e));
		Throwable t = e;
		while (t != null) {
			log(result, "CAUSE", t.getClass().getName() + ": " + (t.getMessage() == null ? "" : t.getMessage()));
			if (t instanceof SQLException) {
				SQLException sql = (SQLException) t;
				log(result, "SQL_STATE", "state=" + sql.getSQLState() + ", errorCode=" + sql.getErrorCode());
				SQLException next = sql.getNextException();
				while (next != null) {
					log(result, "NEXT_SQL_EXCEPTION", next.getClass().getName() + ": " + next.getMessage());
					next = next.getNextException();
				}
			}
			t = t.getCause();
		}
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		try {
			e.printStackTrace(printWriter);
			log(result, "STACKTRACE", abbreviate(stringWriter.toString(), 12000));
		} finally {
			printWriter.close();
		}
	}

	private void addDiagnostic(Result result, String message, String statement) {
		if (result.messages.size() < 25) {
			result.messages.add(message);
		}
		log(result, "ERROR", message);
		log(result, "SQL", abbreviate(statement, 4000));
	}

	/** Mengklasifikasikan {@code statement} sebagai {@code "CREATE TABLE"}, {@code "INSERT"}, atau {@code "statement"} generik, untuk keperluan pesan diagnostik. */
	private String statementKind(String statement) {
		if (statement == null) {
			return "statement";
		}
		String upper = statement.toUpperCase(Locale.ENGLISH);
		if (upper.startsWith("CREATE TABLE")) {
			return "CREATE TABLE";
		}
		if (upper.startsWith("INSERT INTO")) {
			return "INSERT";
		}
		return "statement";
	}

	private String safeTableName(String statement) {
		String table = extractMysqlTableName(statement == null ? "" : statement);
		return table == null ? "-" : table;
	}

	private String abbreviate(String value, int max) {
		if (value == null) {
			return "";
		}
		String compact = value.replace('\r', ' ').replace('\n', ' ');
		return compact.length() <= max ? compact : compact.substring(0, max) + " ...[dipotong]";
	}

	private void update(ProgressListener progressListener, int percent, String message) {
		if (progressListener != null) {
			progressListener.onProgress(percent, message);
		}
	}
}
