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

public class BackupUtil {

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
