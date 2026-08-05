package ais.action.master.helper.impor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.hibernate.Session;
import org.zkoss.zul.Label;
import org.zkoss.zul.Progressmeter;

import com.linuxense.javadbf.DBFField;
import com.linuxense.javadbf.DBFReader;
import com.linuxense.javadbf.DBFWriter;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;

public class ImportFromEpsbedHelper {

	public static String NL = System.getProperty("line.separator");

	public static void main(String[] argv) throws IOException {
		read("mahasiswa.sql");
	}

	@SuppressWarnings("deprecation")
	public static File writeToDBF(List<Object[]> data, String name) throws Exception {
		if (data.size() == 0) {
			return null;
		}
		File dbfFile = new File(Common.REAL_PATH + "/tmp/" + name + ".DBF");
		if (dbfFile.exists()) {
			dbfFile.delete();
		}
		dbfFile.createNewFile();
		DBFWriter dbfWriter = new DBFWriter(dbfFile);

		List<DBFField> dbfFields = new ArrayList<DBFField>();
		for (int i = 0; i < data.get(0).length; i++) {
			DBFField field = new DBFField();
			field.setDataType(DBFField.FIELD_TYPE_C);
			field.setFieldLength(255);
			field.setFieldName(data.get(0)[i] == null ? "" : data.get(0)[i].toString());
			dbfFields.add(field);
		}

		dbfWriter.setFields(dbfFields.toArray(new DBFField[] {}));
		for (Object[] objects : data) {
			dbfWriter.addRecord(objects);
		}

		return dbfFile;
	}

	public static void doImport(File file) {
		doImport(file, null, null, null);
	}

	public static void doImport(File file, Progressmeter progressmeter, Progressmeter progressmeterChild,
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
				String sqlCreateTable = "CREATE TABLE importepsbed.\""
						+ (file.getName().substring(0, file.getName().length() - 4)) + "\" ( " + NL;
				for (int i = 0; i < reader.getFieldCount(); i++) {
					DBFField field = reader.getField(i);

					// field.getDataType()
					if (field.getDataType() == DBFField.FIELD_TYPE_C)
						sqlCreateTable += "\"" + field.getName() + "\" character varying(" + field.getFieldLength()
								+ ")";
					if (field.getDataType() == DBFField.FIELD_TYPE_N || field.getDataType() == DBFField.FIELD_TYPE_M)
						sqlCreateTable += "\"" + field.getName() + "\" numeric";
					if (field.getDataType() == DBFField.FIELD_TYPE_D)
						sqlCreateTable += "\"" + field.getName() + "\" date";
					if (field.getDataType() == DBFField.FIELD_TYPE_F)
						sqlCreateTable += "\"" + field.getName() + "\" double precision";
					if (field.getDataType() == DBFField.FIELD_TYPE_L)
						sqlCreateTable += "\"" + field.getName() + "\" bool";

					if (i != reader.getFieldCount() - 1) {
						sqlCreateTable += "," + NL;
					}

				}
				sqlCreateTable += ");";

				System.out.println("sqlCreateTable = " + sqlCreateTable);
				session.createSQLQuery(sqlCreateTable).executeUpdate();
				session.getTransaction().commit();

				HibernateUtil.closeSession();
			} catch (Exception e1) {
				e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/impor/ImportFromEpsbedHelper.java:105");
			}

			if (progressmeterChild != null) {
				progressmeterChild.setValue(0);
			}
			int rowCount = reader.getRecordCount();
			int row = 0;
			while (rowCount > row) {

				Object rowobj[] = null;
				try {
					rowobj = reader.nextRecord();
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
				if (rowobj == null) {
					row++;
					continue;
				}
				Session session = null;
				try {

					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();

					if (progressmeterChild != null) {
						progressmeterChild.setValue((row * 100) / rowCount);
					}

					if (labelProses != null) {
						labelProses.setValue("Sedang memproses data " + file.getAbsolutePath() + " " + row
								+ " dari total " + rowCount);
					}

					String sqlInsert = "INSERT INTO importepsbed.\""
							+ (file.getName().substring(0, file.getName().length() - 4)) + "\" VALUES ";

					sqlInsert += "(" + NL;
					for (int i = 0; i < reader.getFieldCount(); i++) {
						DBFField field = reader.getField(i);

						if (rowobj[i] == null || rowobj[i].toString().trim().equals("null")) {
							sqlInsert += "null";
						}

						else if (field.getDataType() == DBFField.FIELD_TYPE_C) {
							if (!rowobj[i].toString().trim().equals("")) {

								String data = rowobj[i].toString().trim().replaceAll("'", "");

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
									sqlInsert += "'" + Common.databaseDateFormat.get().format(rowobj[i]) + "'";

								} else {
									sqlInsert += "null";
								}
							} catch (Exception e) {
								sqlInsert += "'" + rowobj[i] + "'";
							}
						}

						if (i != reader.getFieldCount() - 1) {
							sqlInsert += ",";
						}
					}
					sqlInsert += ")";

					sqlInsert += ";";

					// if (row > 0) {

					session.createSQLQuery(sqlInsert).executeUpdate();
					// System.out.println("result = " + result + " sqlInsert = "
					// + sqlInsert);
					// }
					row++;

					session.getTransaction().commit();

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);

					try {
						session.getTransaction().rollback();
					} catch (Exception ee) {
						Common.tampilErrorJikaAdmin(ee);
					}
				}
				HibernateUtil.closeSession();
			}

			if (progressmeterChild != null) {
				progressmeterChild.setValue(100);
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

	}

	public static void importData(String path, Progressmeter progressmeter, Progressmeter progressmeterChild,
			Label labelProses) {

		if (progressmeter != null) {
			progressmeter.setValue(0);
		}

		// try {
		// Session session = HibernateUtil.currentNativeSession();
		// String sql = "";
		// session.getTransaction().begin();
		// sql = "DROP SCHEMA importepsbed;";
		// session.createSQLQuery(sql).executeUpdate();
		// session.getTransaction().commit();
		//
		// HibernateUtil.closeSession();
		// } catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/master/helper/impor/ImportFromEpsbedHelper.java:248");
		// }
		// // DROP TABLE importepsbed.
		try {
			String sql = "CREATE SCHEMA importepsbed;";
			Session session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.createSQLQuery(sql).executeUpdate();
			session.getTransaction().commit();

			HibernateUtil.closeSession();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/impor/ImportFromEpsbedHelper.java:261");
		}

		File fileFolder = new File(path);
		if (!fileFolder.isDirectory()) {
			System.out.println(" file = " + fileFolder);

			if (labelProses != null) {
				labelProses.setValue("Sedang memproses data " + fileFolder.getAbsolutePath());
			}

			if (progressmeter != null) {
				progressmeter.setValue((0 * 100 / 1));
			}

			doImport(fileFolder, progressmeter, progressmeterChild, labelProses);
		} else {
			File[] files = fileFolder.listFiles();
			System.out.println("fileFolder " + fileFolder + " files = " + files.length);
			int urutan = 0;
			for (File file : files) {

				if (file.isDirectory()) {

					ImportFromEpsbedHelper.importData(file.getAbsolutePath(), progressmeter, progressmeterChild,
							labelProses);

				} else {

					if (labelProses != null) {
						labelProses.setValue("Sedang memproses data " + file.getAbsolutePath());
					}

					if (progressmeter != null) {
						progressmeter.setValue((urutan * 100 / files.length));
					}
					urutan++;

					// System.out.println("file = " + file.getAbsolutePath());
					if (!file.getName().toLowerCase().endsWith("dbf")) {
						continue;
					}

					// try {
					// Session session =
					// HibernateUtil.currentNativeSession();
					// String sql = "";
					// session.getTransaction().begin();
					// sql = "DROP TABLE importepsbed.\""
					// + (file.getName().substring(0,
					// file.getName().length() - 4)) + "\";";
					// session.createSQLQuery(sql).executeUpdate();
					// session.getTransaction().commit();
					//
					// HibernateUtil.closeSession();
					// } catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/master/helper/impor/ImportFromEpsbedHelper.java:316");
					// }

					doImport(file, progressmeter, progressmeterChild, labelProses);
				}
			}
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
				ImportFromEpsbedHelper.class.getResourceAsStream("/ais/action/master/helper/impor/" + name), "UTF-8");
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

		labelProses.setValue("Memasukkan data badan hukum");
		String sql = read("badanhukum.sql");
		session.createSQLQuery(sql).executeUpdate();

		labelProses.setValue("Memasukkan data perguruan tinggi");
		sql = read("perguruan_tinggi.sql");
		session.createSQLQuery(sql).executeUpdate();

		labelProses.setValue("Memasukkan data fakultas");
		sql = read("fakultas.sql");
		session.createSQLQuery(sql).executeUpdate();

		labelProses.setValue("Memasukkan data prodi");
		sql = read("jurusan.sql");
		session.createSQLQuery(sql).executeUpdate();

		labelProses.setValue("Memasukkan data jenjang prodi");
		sql = read("jenjang_program_studi.sql");
		session.createSQLQuery(sql).executeUpdate();

		labelProses.setValue("Memasukkan data mahasiswa");
		sql = read("mahasiswa.sql");
		session.createSQLQuery(sql).executeUpdate();

		labelProses.setValue("Memasukkan data dosen");
		sql = read("dosen.sql");
		session.createSQLQuery(sql).executeUpdate();

		labelProses.setValue("Memasukkan data matakuliah");
		sql = read("matakuliah.sql");
		session.createSQLQuery(sql).executeUpdate();

		labelProses.setValue("Memasukkan data kurikulum");
		sql = read("kurikulum.sql");
		session.createSQLQuery(sql).executeUpdate();

		labelProses.setValue("Memasukkan data kurikulum");
		sql = read("kurikulum_punya_matakuliah.sql");
		session.createSQLQuery(sql).executeUpdate();

		labelProses.setValue("Memasukkan data detailperkuliahan");
		sql = read("detailperkuliahan.sql");
		session.createSQLQuery(sql).executeUpdate();

		labelProses.setValue("Memasukkan data formatnilai");
		sql = read("formatnilai.sql");
		session.createSQLQuery(sql).executeUpdate();

		labelProses.setValue("Memasukkan data nilai");
		sql = read("nilai.sql");
		session.createSQLQuery(sql).executeUpdate();

		HibernateUtil.closeSession();

	}

}
