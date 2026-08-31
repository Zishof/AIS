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

/**
 * Helper untuk mengimpor data dari berkas EPSBED (Evaluasi Program Studi Berbasis Evaluasi Diri —
 * sistem pelaporan pendidikan tinggi lama, pendahulu PDDikti/Feeder) ke dalam basis data AIS.
 * Berkas sumber EPSBED berformat DBF (dBASE), satu tabel per berkas (mis. {@code mahasiswa.dbf},
 * {@code dosen.dbf}).
 *
 * <p>
 * Alur kerja utama dipicu dari {@link #importData}: (1) skema sementara {@code importepsbed}
 * dibuat di database; (2) setiap berkas {@code .dbf} pada folder sumber (rekursif ke subfolder)
 * diproses lewat {@link #doImport(File, Progressmeter, Progressmeter, Label)} — tabel staging baru
 * dibuat otomatis di skema {@code importepsbed} dengan nama sama dengan berkas dan kolom bertipe
 * sesuai tipe field DBF, lalu setiap baris DBF disisipkan lewat SQL {@code INSERT} yang dibangun
 * sendiri (satu transaksi native per baris); (3) setelah seluruh berkas selesai, {@link #execute}
 * dipanggil untuk memindahkan data dari skema staging ke tabel produksi AIS lewat rangkaian skrip
 * SQL statis (badan hukum, perguruan tinggi, fakultas, prodi, mahasiswa, dosen, matakuliah,
 * kurikulum, detailperkuliahan, format nilai, nilai — dibaca dari berkas {@code .sql} di paket ini
 * lewat {@link #read(String)}).
 * </p>
 *
 * <p>
 * <b>Catatan keamanan (diperbaiki)</b> — SQL {@code CREATE TABLE} pada
 * {@link #doImport(File, Progressmeter, Progressmeter, Label)} tetap dibangun lewat konkatenasi
 * string (identifier SQL tidak bisa diikat sebagai parameter bind), namun nama tabel (dari nama
 * berkas DBF) dan nama kolom (dari nama field DBF) kini divalidasi lewat
 * {@link #sanitizeIdentifier(String, String)} — hanya huruf/angka/garis bawah diterima, selain
 * itu {@link IllegalArgumentException} dilempar dan impor berkas tersebut gagal. Nilai kolom pada
 * {@code INSERT} tidak lagi disisipkan sebagai literal string; kini diikat lewat parameter posisi
 * ({@code ?}) memakai {@code SQLQuery.setParameter(int, Object)}. Sebelumnya kedua celah ini
 * berpotensi SQL injection/DDL injection ke skema {@code importepsbed} bila nama berkas atau isi
 * berkas DBF yang diimpor berasal dari sumber tidak tepercaya. Progress meter dan label diberikan
 * opsional untuk menampilkan kemajuan proses ke UI (dipanggil dari komponen admin), namun method
 * inti tetap dapat dipakai tanpa UI (parameter {@code null}).
 * </p>
 */
public class ImportFromEpsbedHelper {

	public static String NL = System.getProperty("line.separator");

	/**
	 * Memvalidasi bahwa {@code name} berupa identifier SQL aman (hanya huruf/angka/garis bawah)
	 * sebelum dipakai dalam DDL/DML yang dirakit lewat konkatenasi string — nama tabel/kolom tidak
	 * bisa diikat sebagai parameter bind seperti nilai data biasa. Menolak identifier kosong atau
	 * yang mengandung karakter di luar {@code [A-Za-z0-9_]} untuk mencegah SQL/DDL injection lewat
	 * nama berkas DBF atau nama field DBF yang berasal dari sumber tidak tepercaya.
	 *
	 * @param name    identifier yang akan divalidasi
	 * @param konteks label sumber identifier untuk pesan galat (mis. "nama tabel", "nama kolom")
	 * @return {@code name} apa adanya, bila valid
	 * @throws IllegalArgumentException bila {@code name} kosong atau mengandung karakter tidak aman
	 */
	private static String sanitizeIdentifier(String name, String konteks) {
		if (name == null || !name.matches("[A-Za-z0-9_]+")) {
			throw new IllegalArgumentException("Impor EPSBED ditolak: " + konteks
					+ " mengandung karakter yang tidak diperbolehkan (hanya huruf, angka, dan garis bawah diizinkan): "
					+ name);
		}
		return name;
	}

	/** Titik masuk baris perintah untuk pengujian manual: membaca isi {@code mahasiswa.sql} dan mencetaknya. */
	public static void main(String[] argv) throws IOException {
		read("mahasiswa.sql");
	}

	/**
	 * Menulis {@code data} (baris pertama dipakai sebagai nama kolom, semua kolom dibuat bertipe
	 * karakter panjang 255) ke berkas DBF baru di {@code Common.REAL_PATH + "/tmp/" + name + ".DBF"},
	 * menimpa berkas lama bila sudah ada.
	 *
	 * @param data daftar baris (baris ke-0 dipakai sebagai header nama kolom)
	 * @param name nama berkas (tanpa ekstensi)
	 * @return berkas DBF yang ditulis, atau {@code null} bila {@code data} kosong
	 * @throws Exception diteruskan dari kegagalan I/O atau penulisan DBF
	 */
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

	/** Seperti {@link #doImport(File, Progressmeter, Progressmeter, Label)} tanpa indikator progres UI. */
	public static void doImport(File file) {
		doImport(file, null, null, null);
	}

	/**
	 * Mengimpor satu berkas DBF ke tabel staging baru pada skema {@code importepsbed} (nama tabel
	 * = nama berkas tanpa ekstensi {@code .dbf}, dibuat ulang dari struktur field DBF), lalu
	 * menyisipkan seluruh baris satu per satu, masing-masing dalam transaksi native tersendiri
	 * (kegagalan satu baris di-rollback dan tidak menghentikan baris berikutnya). Lihat javadoc
	 * kelas untuk catatan keamanan terkait pembangunan SQL lewat konkatenasi string.
	 *
	 * @param file                berkas {@code .dbf} sumber
	 * @param progressmeter       indikator progres keseluruhan (opsional, boleh {@code null})
	 * @param progressmeterChild  indikator progres per-baris berkas ini (opsional, boleh {@code null})
	 * @param labelProses         label status teks yang diperbarui selama proses (opsional, boleh {@code null})
	 */
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
				String tableName = sanitizeIdentifier(file.getName().substring(0, file.getName().length() - 4),
						"nama tabel (dari nama berkas)");
				String sqlCreateTable = "CREATE TABLE importepsbed.\""
						+ tableName + "\" ( " + NL;
				for (int i = 0; i < reader.getFieldCount(); i++) {
					DBFField field = reader.getField(i);
					String fieldName = sanitizeIdentifier(field.getName(), "nama kolom (dari field DBF)");

					// field.getDataType()
					if (field.getDataType() == DBFField.FIELD_TYPE_C)
						sqlCreateTable += "\"" + fieldName + "\" character varying(" + field.getFieldLength()
								+ ")";
					if (field.getDataType() == DBFField.FIELD_TYPE_N || field.getDataType() == DBFField.FIELD_TYPE_M)
						sqlCreateTable += "\"" + fieldName + "\" numeric";
					if (field.getDataType() == DBFField.FIELD_TYPE_D)
						sqlCreateTable += "\"" + fieldName + "\" date";
					if (field.getDataType() == DBFField.FIELD_TYPE_F)
						sqlCreateTable += "\"" + fieldName + "\" double precision";
					if (field.getDataType() == DBFField.FIELD_TYPE_L)
						sqlCreateTable += "\"" + fieldName + "\" bool";

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

					String tableName = sanitizeIdentifier(file.getName().substring(0, file.getName().length() - 4),
							"nama tabel (dari nama berkas)");
					String sqlInsert = "INSERT INTO importepsbed.\""
							+ tableName + "\" VALUES ";

					sqlInsert += "(" + NL;
					List<Object> insertParams = new ArrayList<Object>();
					for (int i = 0; i < reader.getFieldCount(); i++) {
						DBFField field = reader.getField(i);

						if (rowobj[i] == null || rowobj[i].toString().trim().equals("null")) {
							sqlInsert += "null";
						}

						else if (field.getDataType() == DBFField.FIELD_TYPE_C) {
							if (!rowobj[i].toString().trim().equals("")) {
								sqlInsert += "?";
								insertParams.add(rowobj[i].toString().trim());
							} else {
								sqlInsert += "null";
							}
						} else if (field.getDataType() == DBFField.FIELD_TYPE_N
								|| field.getDataType() == DBFField.FIELD_TYPE_M) {
							if (!rowobj[i].toString().trim().equals("")) {
								sqlInsert += "?";
								insertParams.add(rowobj[i]);
							} else {
								sqlInsert += "null";
							}
						} else if (field.getDataType() == DBFField.FIELD_TYPE_F) {
							if (!rowobj[i].toString().trim().equals("")) {
								sqlInsert += "?";
								insertParams.add(rowobj[i]);
							} else {
								sqlInsert += "null";
							}
						} else if (field.getDataType() == DBFField.FIELD_TYPE_L) {
							if (!rowobj[i].toString().trim().equals("")) {
								sqlInsert += "?";
								insertParams.add(rowobj[i]);
							} else {
								sqlInsert += "null";
							}
						} else if (field.getDataType() == DBFField.FIELD_TYPE_D) {
							if (!rowobj[i].toString().trim().equals("")) {
								sqlInsert += "?";
								if (rowobj[i] instanceof java.util.Date) {
									insertParams.add(new java.sql.Date(((java.util.Date) rowobj[i]).getTime()));
								} else {
									insertParams.add(rowobj[i].toString());
								}
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

					org.hibernate.SQLQuery insertQuery = session.createSQLQuery(sqlInsert);
					for (int p = 0; p < insertParams.size(); p++) {
						insertQuery.setParameter(p, insertParams.get(p));
					}
					insertQuery.executeUpdate();
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

	/**
	 * Titik masuk utama proses impor EPSBED: membuat skema {@code importepsbed}, lalu memproses
	 * {@code path} secara rekursif — bila berupa folder, setiap subfolder diproses ulang lewat
	 * pemanggilan rekursif dan setiap berkas {@code .dbf} langsung diimpor lewat
	 * {@link #doImport(File, Progressmeter, Progressmeter, Label)}; bila {@code path} langsung
	 * menunjuk satu berkas, berkas itu saja yang diimpor. Setelah seluruh berkas selesai, memanggil
	 * {@link #execute(Label)} untuk memindahkan data staging ke tabel produksi.
	 *
	 * @param path                path berkas {@code .dbf} tunggal atau folder berisi berkas-berkas DBF
	 * @param progressmeter       indikator progres keseluruhan antar berkas/folder (opsional)
	 * @param progressmeterChild  diteruskan ke {@link #doImport} untuk progres per baris (opsional)
	 * @param labelProses         label status teks (opsional)
	 */
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

	/**
	 * Membaca isi berkas teks (skrip SQL) {@code name} dari classpath paket ini
	 * ({@code /ais/action/master/helper/impor/}) sebagai satu string UTF-8, baris demi baris.
	 *
	 * @param name nama berkas relatif terhadap paket ini (mis. {@code "mahasiswa.sql"})
	 * @return isi lengkap berkas
	 * @throws IOException tidak pernah dilempar secara eksplisit di implementasi ini (dideklarasikan
	 *                      pada signature); kegagalan stream {@code null} akan menyebabkan NPE
	 */
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

	/**
	 * Menjalankan rangkaian skrip SQL statis (dibaca lewat {@link #read(String)} dari berkas
	 * {@code .sql} di paket ini) yang memindahkan data hasil staging ke tabel produksi AIS, dalam
	 * urutan tetap: badan hukum, perguruan tinggi, fakultas, prodi, jenjang prodi, mahasiswa,
	 * dosen, matakuliah, kurikulum, kurikulum-punya-matakuliah, detailperkuliahan, format nilai,
	 * lalu nilai. {@code labelProses} diperbarui sebelum tiap tahap untuk menunjukkan progres ke UI.
	 *
	 * @param labelProses label status teks yang diperbarui di setiap tahap; TIDAK boleh
	 *                    {@code null} (dipanggil langsung tanpa pengecekan null)
	 * @throws Exception diteruskan dari kegagalan membaca berkas {@code .sql} atau eksekusi SQL
	 */
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
