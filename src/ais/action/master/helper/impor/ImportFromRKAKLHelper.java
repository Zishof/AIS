package ais.action.master.helper.impor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.hibernate.Session;
import org.zkoss.zul.Label;
import org.zkoss.zul.Progressmeter;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;

import com.linuxense.javadbf.DBFField;
import com.linuxense.javadbf.DBFReader;
import com.linuxense.javadbf.DBFWriter;

public class ImportFromRKAKLHelper {

	public static String NL = System.getProperty("line.separator");

	public static SimpleDateFormat dateFormat = new SimpleDateFormat(
			"yyyy-MM-dd");

	public static void main(String[] argv) throws IOException {
		read("mahasiswa.sql");
	}

	@SuppressWarnings("deprecation")
	public static File writeToDBF(List<Object[]> data, String name)
			throws Exception {
		if (data.size() == 0) {
			return null;
		}
		File keuFile = new File(Common.REAL_PATH + "/tmp/" + name + ".KEU");
		if (keuFile.exists()) {
			keuFile.delete();
		}
		keuFile.createNewFile();
		DBFWriter keuWriter = new DBFWriter(keuFile);

		List<DBFField> keuFields = new ArrayList<DBFField>();
		for (int i = 0; i < data.get(0).length; i++) {
			DBFField field = new DBFField();
			field.setDataType(DBFField.FIELD_TYPE_C);
			field.setFieldLength(255);
			field.setFieldName(data.get(0)[i] == null ? "" : data.get(0)[i]
					.toString());
			keuFields.add(field);
		}

		keuWriter.setFields(keuFields.toArray(new DBFField[] {}));
		for (Object[] objects : data) {
			keuWriter.addRecord(objects);
		}

		return keuFile;
	}

	public static void doImport(File file,
			Progressmeter progressmeter, Progressmeter progressmeterChild,
			Label labelProses) {
		try {

			DBFReader reader;
			FileInputStream inputstream = new FileInputStream(file);
			reader = new DBFReader(inputstream);
			System.out.println("Daftar Nama Fields");
			System.out.println("==================");
			try {
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				String sqlCreateTable = "CREATE TABLE rab_import.\""
						+ (file.getName().substring(0,
								file.getName().length() - 4)) + "\" ( " + NL;
				String filed = "";
				for (int i = 0; i < reader.getFieldCount(); i++) {
					DBFField field = reader.getField(i);

					// field.getDataType()
					if (field.getDataType() == DBFField.FIELD_TYPE_C)
						filed += filed.equals("") ? "\"" + field.getName()
								+ "\" character varying("
								+ field.getFieldLength() + ")" : ",\""
								+ field.getName() + "\" character varying("
								+ field.getFieldLength() + ")";
					else if (field.getDataType() == DBFField.FIELD_TYPE_N
							|| field.getDataType() == DBFField.FIELD_TYPE_M)
						filed += filed.equals("") ? "\"" + field.getName()
								+ "\" numeric" : ",\"" + field.getName()
								+ "\" numeric";
					else if (field.getDataType() == DBFField.FIELD_TYPE_D)
						filed += filed.equals("") ? "\"" + field.getName()
								+ "\" date" : ",\"" + field.getName()
								+ "\" date";
					else if (field.getDataType() == DBFField.FIELD_TYPE_F)
						filed += filed.equals("") ? "\"" + field.getName()
								+ "\" double precision" : ",\""
								+ field.getName() + "\" double precision";
					else if (field.getDataType() == DBFField.FIELD_TYPE_L)
						filed += filed.equals("") ? "\"" + field.getName()
								+ "\" bool" : ",\"" + field.getName()
								+ "\" bool";
					else {
						filed += filed.equals("") ? "\"" + field.getName()
								+ "\" character varying("
								+ field.getFieldLength() + ")" : ",\""
								+ field.getName() + "\" character varying("
								+ field.getFieldLength() + ")";
					}

					// filed += NL;

					// if (i != reader.getFieldCount() - 1) {
					// sqlCreateTable += "," + NL;
					// }

				}
				sqlCreateTable += filed;
				sqlCreateTable += ");";

				System.out.println("sqlCreateTable = " + sqlCreateTable);
				if (!filed.trim().equals("")) {
					session.createSQLQuery(sqlCreateTable).executeUpdate();
				}
				session.getTransaction().commit();

				
				HibernateUtil.closeSession();
			} catch (Exception e1) {
				e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/impor/ImportFromRKAKLHelper.java:135");
				// JANGAN System.exit() — itu mematikan SELURUH JVM Tomcat (server ikut berhenti).
				// Cukup batalkan transaksi, tutup session, beri pesan, lalu hentikan impor ini saja.
				try {
					HibernateUtil.currentNativeSession().getTransaction().rollback();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/impor/ImportFromRKAKLHelper.java:140");
				}
				try {
					HibernateUtil.closeSession();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/impor/ImportFromRKAKLHelper.java:144");
				}
				if (labelProses != null) {
					try {
						labelProses.setValue("Gagal membuat tabel impor RKAKL: " + e1.getMessage());
					} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/impor/ImportFromRKAKLHelper.java:149");
					}
				}
				return;
			}

			if (progressmeterChild != null) {
				progressmeterChild.setValue(0);
			}
			int rowCount = reader.getRecordCount();
			int row = 0;
			while (true) {
				Object rowobj[] = reader.nextRecord();
				if (rowobj == null)
					break;

				try {

					if (progressmeterChild != null) {
						progressmeterChild.setValue((row * 100) / rowCount);
					}

					String sqlInsert = "INSERT INTO rab_import.\""
							+ (file.getName().substring(0, file.getName()
									.length() - 4)) + "\" VALUES ";

					sqlInsert += "(" + NL;
					for (int i = 0; i < reader.getFieldCount(); i++) {
						DBFField field = reader.getField(i);

						if (rowobj[i] == null
								|| rowobj[i].toString().trim().equals("null")) {
							sqlInsert += "null";
						}

						else if (field.getDataType() == DBFField.FIELD_TYPE_C) {
							if (!rowobj[i].toString().trim().equals("")) {

								String data = rowobj[i].toString().trim()
										.replaceAll("'", "");

								sqlInsert += "'" + data + "'";
							} else {
								sqlInsert += "null";
							}
						} else if (field.getDataType() == DBFField.FIELD_TYPE_N
								|| field.getDataType() == DBFField.FIELD_TYPE_M) {
							if (!rowobj[i].toString().trim().equals("")) {
								sqlInsert += "" + rowobj[i] + "";
							} else {
								sqlInsert += "null";
							}
						} else if (field.getDataType() == DBFField.FIELD_TYPE_F) {
							if (!rowobj[i].toString().trim().equals("")) {
								sqlInsert += "" + rowobj[i] + "";
							} else {
								sqlInsert += "null";
							}
						} else if (field.getDataType() == DBFField.FIELD_TYPE_L) {
							if (!rowobj[i].toString().trim().equals("")) {
								sqlInsert += "" + rowobj[i] + "";
							} else {
								sqlInsert += "null";
							}
						} else if (field.getDataType() == DBFField.FIELD_TYPE_D) {
							try {
								if (!rowobj[i].toString().trim().equals("")) {
									sqlInsert += "'"
											+ dateFormat.format(rowobj[i])
											+ "'";

								} else {
									sqlInsert += "null";
								}
							} catch (Exception e) {
								sqlInsert += "'" + rowobj[i] + "'";
							}
						} else {
							if (!rowobj[i].toString().trim().equals("")) {
								sqlInsert += "" + rowobj[i] + "";
							} else {
								sqlInsert += "null";
							}
						}

						if (i != reader.getFieldCount() - 1) {
							sqlInsert += ",";
						}
					}
					sqlInsert += ")";

					sqlInsert += ";";

					// if (row > 0) {
					System.out.println("sqlInsert = " + sqlInsert);
					Session session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.createSQLQuery(sqlInsert).executeUpdate();
					session.getTransaction().commit();
					
					HibernateUtil.closeSession();
					// }
					row++;

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e); 
				}
			}

			if (progressmeterChild != null) {
				progressmeterChild.setValue(100);
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}

	}

	public static void importData(String path,
			Progressmeter progressmeter, Progressmeter progressmeterChild,
			Label labelProses) {

		if (progressmeter != null) {
			progressmeter.setValue(0);
		}
		Session session = HibernateUtil.currentNativeSession();
		try {

			String sql = "";
			session.getTransaction().begin();
			sql = "DROP SCHEMA rab_import;";
			session.createSQLQuery(sql).executeUpdate();
			session.getTransaction().commit();

		} catch (Exception e1) {
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/impor/ImportFromRKAKLHelper.java:285");
		}
		if (session.isOpen()) {
			
			HibernateUtil.closeSession();
		}
		try {
			String sql = "CREATE SCHEMA rab_import;";
			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.createSQLQuery(sql).executeUpdate();
			session.getTransaction().commit();

			
			HibernateUtil.closeSession();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/impor/ImportFromRKAKLHelper.java:302");
		}
		if (session.isOpen()) {
			
			HibernateUtil.closeSession();
		}

		File fileFolder = new File(path);
		File[] files = fileFolder.listFiles();
		int urutan = 0;
		for (File file : files) {

			if (labelProses != null) {
				labelProses.setValue("Sedang memproses data "
						+ file.getAbsolutePath());
			}

			if (progressmeter != null) {
				progressmeter.setValue((urutan * 100 / files.length));
			}
			urutan++;

			// System.out.println("file = " + file.getAbsolutePath());
			if (!file.getName().toLowerCase().endsWith("keu")
					|| file.getName().toLowerCase().contains("log")
					|| file.getName().toLowerCase().contains("t_cek")) {
				continue;
			}

			// try {
			// session = HibernateUtil.currentNativeSession();
			// String sql = "";
			// session.getTransaction().begin();
			// sql = "DROP TABLE rab_import.\""
			// + (file.getName().substring(0,
			// file.getName().length() - 4)) + "\";";
			// session.createSQLQuery(sql).executeUpdate();
			// session.getTransaction().commit();
			
			// HibernateUtil.closeSession();
			// } catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/master/helper/impor/ImportFromRKAKLHelper.java:342");
			
			// HibernateUtil.closeSession();
			// e1.printStackTrace();
			// }

			doImport(file, progressmeter, progressmeterChild, labelProses);
		}

		try {
			execute(labelProses);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}

		if (progressmeter != null) {
			progressmeter.setValue(100);
		}
	}

	public static String read(String name) throws IOException {
		StringBuilder text = new StringBuilder();
		String NL = System.getProperty("line.separator");
		Scanner scanner = new Scanner(
				ImportFromRKAKLHelper.class.getResourceAsStream("/ais/action/master/helper/impor/"
						+ name), "UTF-8");
		try {
			while (scanner.hasNextLine()) {
				text.append(scanner.nextLine() + NL);
			}
		} finally {
			scanner.close();
		}
		System.out.println("Text read in: " + text);
		return text.toString();
	}

	public static void execute(Label labelProses) throws Exception {
		Session session = HibernateUtil.currentNativeSession();

		
		HibernateUtil.closeSession();

	}

}
