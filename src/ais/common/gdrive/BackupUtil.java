package ais.common.gdrive;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.impl.SessionFactoryImpl;
import org.zkoss.zul.Label;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;

/**
 * Utilitas pencadangan (backup) basis data PostgreSQL AIS ke berkas lokal memakai perkakas
 * baris perintah {@code pg_dump}, beserta pembersihan berkas cadangan lama. Meskipun berada di
 * paket {@code ais.common.gdrive} (mengindikasikan rencana/tujuan akhir integrasi dengan Google
 * Drive), kelas ini SENDIRI <b>tidak melakukan panggilan API Google Drive apa pun</b> — tidak ada
 * OAuth, service account key, maupun token Google yang tertanam di sini. Seluruh pekerjaan kelas
 * ini murni lokal: membaca kredensial koneksi basis data yang sedang dipakai aplikasi (lewat
 * refleksi terhadap objek internal Hibernate), menjalankan {@code pg_dump} sebagai proses
 * eksternal, dan menghapus berkas {@code .backup} lama dari direktori cadangan. Kemungkinan besar
 * berkas hasil {@link #backupPGSQL(Label)}/{@link #backupPGSQLStream(Label)} diunggah ke Google
 * Drive oleh kelas lain di paket ini atau di luar paket ini (tidak tercakup dalam file ini).
 *
 * <p>
 * <b>Sumber kredensial basis data</b> — kelas ini TIDAK menyimpan kredensial basis data sendiri.
 * Alih-alih, ia mengambil kredensial (URL JDBC, user, password) yang SEDANG dipakai aplikasi
 * secara dinamis lewat refleksi ({@link Field#setAccessible(boolean)}) terhadap field privat
 * {@code properties} pada {@link SessionFactoryImpl}/{@link SessionFactory} milik Hibernate, lalu
 * membaca properti {@code hibernate.connection.url}/{@code username}/{@code password} (dengan
 * fallback ke properti alternatif tak-ter-substitusi {@code ${url}}/{@code ${username}}/
 * {@code ${password}}, dan varian {@code _streaming} untuk sumber data streaming). Dengan
 * demikian TIDAK ADA kredensial basis data yang tertanam langsung di kode sumber kelas ini —
 * kredensial berasal dari konfigurasi Hibernate aplikasi (biasanya {@code hibernate.cfg.xml} atau
 * berkas properti terpisah di luar cakupan file ini) pada saat runtime.
 * </p>
 *
 * <p>
 * <b>Penanganan password saat eksekusi {@code pg_dump}</b> — password basis data yang dibaca
 * lewat refleksi diteruskan ke proses {@code pg_dump} melalui variabel lingkungan
 * {@code PGPASSWORD} (bukan sebagai argumen baris perintah, yang dapat terekspos lewat daftar
 * proses OS), dan secara eksplisit TIDAK dicetak ke log ({@code System.out}) — komentar kode
 * sumber {@code "KEAMANAN: password DB tidak dicetak ke log"} menandai praktik ini sebagai
 * kesengajaan. IP host, nama basis data, dan user tetap dicetak ke {@code System.out} untuk
 * keperluan diagnosis, namun ini dianggap informasi berisiko rendah dibanding password.
 * </p>
 *
 * <p>
 * <b>Catatan potensi masalah pada {@link #deleteDatabase()}</b> — method ini membentuk perintah
 * {@code rm -rf <direktori>*.backup} sebagai SATU string tunggal dan menyerahkannya ke
 * {@code new ProcessBuilder(String)} (constructor argumen tunggal, yang memperlakukan seluruh
 * string sebagai NAMA PROGRAM, bukan sebagai perintah shell yang di-parse ulang menjadi
 * program+argumen terpisah, dan TIDAK melakukan ekspansi wildcard {@code *} karena tidak ada
 * shell yang menafsirkannya). Pada platform Unix umumnya hal ini akan gagal dijalankan (OS
 * mencari executable bernama literal {@code "rm -rf /backup/*.backup"} yang tidak ada), sehingga
 * method ini kemungkinan besar TIDAK benar-benar menghapus berkas cadangan lama pada
 * implementasi saat ini — perilaku ini tidak diubah di sini karena instruksi dokumentasi hanya
 * mencakup penambahan Javadoc, bukan perbaikan logika; lihat catatan pada laporan dokumentasi.
 * </p>
 */
public class BackupUtil {

	/**
	 * Membuat cadangan basis data PostgreSQL utama (non-streaming, diambil dari
	 * {@link HibernateUtil#getSessionFactory()}) memakai {@code pg_dump} dengan format custom
	 * terkompresi ({@code -F c -Z 9}), dan menampilkan progres baris-demi-baris output
	 * {@code pg_dump} ke komponen {@link Label} ZK bila diberikan.
	 *
	 * <p>
	 * Urutan kerja: (1) memastikan direktori cadangan (konfigurasi
	 * {@code lokasi_directory_file_backup}, default {@code "/backup/"}) ada; (2) membaca
	 * kredensial koneksi aktif lewat refleksi terhadap {@link SessionFactoryImpl}; (3) menyusun
	 * nama berkas cadangan dari label universitas (konfigurasi {@code label_universitas}), nama
	 * basis data, dan tanggal saat ini; (4) menghapus berkas lama bernama sama bila ada, lalu
	 * membuat berkas baru; (5) menjalankan {@code pg_dump} (lokasi biner dari konfigurasi
	 * {@code lokasi_pg_dump}, default {@code "pg_dump"}) sebagai proses eksternal dengan password
	 * diteruskan lewat variabel lingkungan {@code PGPASSWORD}; (6) membaca output proses baris
	 * demi baris, mencetaknya ke {@code System.out}, dan memperbarui {@code label} bila diberikan.
	 * </p>
	 *
	 * @param label komponen label ZK opsional untuk menampilkan progres/output {@code pg_dump}
	 *              secara langsung ke antarmuka; boleh {@code null} bila tidak diperlukan
	 * @return berkas hasil cadangan yang dibuat (objek {@link File}, tidak dijamin proses
	 *         {@code pg_dump} sudah berhasil sepenuhnya — status keberhasilan hanya terlihat dari
	 *         output proses); {@code null} bila terjadi kegagalan sebelum berkas sempat dibuat
	 */
	public static File backupPGSQL(Label label) {
		File backupFile = null;
		try {
			String backupDirectory = Common.getKonfigurasi("lokasi_directory_file_backup", "/backup/").getNilai();
			new File(backupDirectory).mkdirs();

			Field f = SessionFactoryImpl.class.getDeclaredField("properties");
			f.setAccessible(true);
			Properties properties = (Properties) f.get(HibernateUtil.getSessionFactory());

			System.out.println(properties);

			// PostgreSQL variables
			String url = properties.getProperty("hibernate.connection.url").trim().equalsIgnoreCase("${url}")
					? properties.getProperty("url").trim()
					: properties.getProperty("hibernate.connection.url").trim();

			String IP = url.replaceAll("jdbc:postgresql://", "").split(":")[0];
			String user = properties.getProperty("hibernate.connection.username").trim().equalsIgnoreCase("${username}")
					? properties.getProperty("username").trim()
					: properties.getProperty("hibernate.connection.username").trim();

			String[] dbbb = url.split("/");
			String dbase = dbbb[dbbb.length - 1].trim();
			String password = properties.getProperty("hibernate.connection.password").trim()
					.equalsIgnoreCase("${password}") ? properties.getProperty("password").trim()
							: properties.getProperty("hibernate.connection.password").trim();

			System.out.println("IP : " + IP);
			System.out.println("dbase : " + dbase);
			System.out.println("user : " + user);
			// KEAMANAN: password DB tidak dicetak ke log.

			Process p;
			ProcessBuilder pb;

			String label_universitas = Common.getKonfigurasi("label_universitas", "_").getNilai().replaceAll(" ", "_");

			String fileName = label_universitas + "_" + dbase + "_DB_"
					+ Common.dateFormat1.get().format(ais.ui.util.WaktuUtil.getDate()) + ".backup";
			backupFile = new File(
					backupDirectory.endsWith("/") ? backupDirectory + fileName : backupDirectory + "/" + fileName);

			System.out.println("backupFile = " + backupFile.getAbsolutePath());
			if (backupFile.exists()) {
				backupFile.delete();
			}

			backupFile.getParentFile().mkdirs();
			backupFile.createNewFile();

			pb = new ProcessBuilder(Common.getKonfigurasi("lokasi_pg_dump", "pg_dump").getNilai(), "-f",
					backupFile.getAbsolutePath(), "-F", "c", "-Z", "9", "-v", "-h", IP, "-U", user, dbase);
			pb.environment().put("PGPASSWORD", password);
			pb.redirectErrorStream(true);
			p = pb.start();
			try {
				InputStream is = p.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String ll;
				while ((ll = br.readLine()) != null) {
					System.out.println(ll);
					if (label != null)
						label.setValue(ll);
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

	

		} catch (Exception x) {
			x.printStackTrace(); ais.common.ErrorAuditUtil.record(x, "auto-audit src/ais/common/gdrive/BackupUtil.java:94");
			System.err.println("Could not invoke browser, command=");
			System.err.println("Caught: " + x.getMessage());
		}

		return backupFile;
	}

	/**
	 * Sama seperti {@link #backupPGSQL(Label)}, namun mencadangkan basis data PostgreSQL yang
	 * dipakai untuk keperluan <b>streaming</b> (sumber data replikasi/streaming terpisah, diambil
	 * dari {@link StreamingHibernateUtil#getInstance()} alih-alih {@link HibernateUtil} utama).
	 * Kredensial dibaca lewat refleksi terhadap {@link SessionFactory} milik sesi streaming
	 * tersebut, dengan fallback ke properti varian {@code _streaming}
	 * ({@code url_streaming}/{@code username_streaming}/{@code password_streaming}) bila properti
	 * standar belum tersubstitusi. Seluruh langkah kerja lainnya (penamaan berkas, eksekusi
	 * {@code pg_dump}, penanganan password lewat {@code PGPASSWORD}, pembacaan progres) identik
	 * dengan {@link #backupPGSQL(Label)}.
	 *
	 * @param label komponen label ZK opsional untuk menampilkan progres/output {@code pg_dump};
	 *              boleh {@code null}
	 * @return berkas hasil cadangan basis data streaming, atau {@code null} bila terjadi
	 *         kegagalan sebelum berkas sempat dibuat
	 */
	public static File backupPGSQLStream(Label label) {
		File backupFile = null;
		try {
			String backupDirectory = Common.getKonfigurasi("lokasi_directory_file_backup", "/backup/").getNilai();
			new File(backupDirectory).mkdirs();

			Field f = SessionFactory.class.getDeclaredField("properties");
			f.setAccessible(true);
			Properties properties = (Properties) f.get(StreamingHibernateUtil.getInstance().getSessionFactory());

			System.out.println(properties);

			// PostgreSQL variables
			String url = properties.getProperty("hibernate.connection.url").trim().equalsIgnoreCase("${url_streaming}")
					? properties.getProperty("url_streaming").trim()
					: properties.getProperty("hibernate.connection.url").trim();

			String IP = url.replaceAll("jdbc:postgresql://", "").split(":")[0];
			String user = properties.getProperty("hibernate.connection.username").trim()
					.equalsIgnoreCase("${username_streaming}") ? properties.getProperty("username_streaming").trim()
							: properties.getProperty("hibernate.connection.username").trim();

			String[] dbbb = url.split("/");
			String dbase = dbbb[dbbb.length - 1].trim();
			String password = properties.getProperty("hibernate.connection.password").trim()
					.equalsIgnoreCase("${password_streaming}") ? properties.getProperty("password_streaming").trim()
							: properties.getProperty("hibernate.connection.password").trim();

			System.out.println("IP : " + IP);
			System.out.println("dbase : " + dbase);
			System.out.println("user : " + user);
			// KEAMANAN: password DB tidak dicetak ke log.

			Process p;
			ProcessBuilder pb;

			String label_universitas = Common.getKonfigurasi("label_universitas", "_").getNilai().replaceAll(" ", "_");

			String fileName = label_universitas + "_" + dbase + "_DB_"
					+ Common.dateFormat1.get().format(ais.ui.util.WaktuUtil.getDate()) + ".backup";
			backupFile = new File(
					backupDirectory.endsWith("/") ? backupDirectory + fileName : backupDirectory + "/" + fileName);

			System.out.println("backupFile = " + backupFile.getAbsolutePath());
			if (backupFile.exists()) {
				backupFile.delete();
			}

			backupFile.getParentFile().mkdirs();
			backupFile.createNewFile();

			pb = new ProcessBuilder(Common.getKonfigurasi("lokasi_pg_dump", "pg_dump").getNilai(), "-f",
					backupFile.getAbsolutePath(), "-F", "c", "-Z", "9", "-v", "-h", IP, "-U", user, dbase);
			pb.environment().put("PGPASSWORD", password);
			pb.redirectErrorStream(true);
			p = pb.start();
			try {
				InputStream is = p.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String ll;
				while ((ll = br.readLine()) != null) {
					System.out.println(ll);
					if (label != null)
						label.setValue(ll);
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

		} catch (Exception x) {
			x.printStackTrace(); ais.common.ErrorAuditUtil.record(x, "auto-audit src/ais/common/gdrive/BackupUtil.java:173");
			System.err.println("Could not invoke browser, command=");
			System.err.println("Caught: " + x.getMessage());
		}

		return backupFile;
	}

	/**
	 * Bermaksud menghapus seluruh berkas cadangan lama (berekstensi {@code .backup}) pada
	 * direktori cadangan (konfigurasi {@code lokasi_directory_file_backup}, default
	 * {@code "/backup/"}) dengan menjalankan perintah {@code rm -rf <direktori>*.backup} lewat
	 * {@link ProcessBuilder}. <b>Lihat catatan pada Javadoc kelas</b> — cara pembentukan
	 * {@link ProcessBuilder} pada implementasi saat ini (constructor argumen tunggal berisi
	 * seluruh perintah sebagai satu string, tanpa shell yang menafsirkan spasi/wildcard) kemungkinan
	 * besar membuat perintah ini gagal dieksekusi sebagaimana dimaksud pada platform Unix.
	 * Kegagalan proses (termasuk {@link IOException} saat membaca output) ditangkap dan
	 * ditampilkan lewat {@link Common#tampilErrorJikaAdmin(Exception)}, bukan dilempar ke
	 * pemanggil.
	 */
	public static void deleteDatabase() {
		try {
			String backupDirectory = Common.getKonfigurasi("lokasi_directory_file_backup", "/backup/").getNilai();

			Process p;
			ProcessBuilder pb;

			// String perintah = "find " + (backupDirectory.endsWith("/") ?
			// backupDirectory : backupDirectory + "/")
			// + "*.backup -mtime +1 -exec rm {} \\;";
			// System.out.println("perintah => " + perintah);
			pb = new ProcessBuilder(
					"rm -rf " + (backupDirectory.endsWith("/") ? backupDirectory : backupDirectory + "/") + "*.backup");

			pb.redirectErrorStream(true);
			p = pb.start();

			try {
				InputStream is = p.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String ll;
				while ((ll = br.readLine()) != null) {
					System.out.println(ll);
				}
			} catch (IOException e) {
				Common.tampilErrorJikaAdmin(e);
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			System.err.println("Could not invoke browser, command=");
			System.err.println("Caught: " + e.getMessage());
		}
	}

}
