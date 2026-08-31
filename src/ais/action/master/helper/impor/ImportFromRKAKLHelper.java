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

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;

import com.linuxense.javadbf.DBFField;
import com.linuxense.javadbf.DBFReader;
import com.linuxense.javadbf.DBFWriter;

/**
 * Helper impor data dari sistem RKAKL (Rencana Kerja Anggaran Kementerian/Lembaga — sistem
 * anggaran instansi pemerintah Indonesia) yang mengekspor datanya dalam berkas DBF (format
 * dBASE, ekstensi {@code .KEU}). Alur kerja utama ({@link #importData}): (1) hapus lalu buat
 * ulang skema PostgreSQL {@code rab_import} sebagai area kerja sementara; (2) untuk setiap
 * berkas {@code .KEU} pada folder sumber, buat tabel baru di skema tersebut dengan struktur
 * kolom mengikuti field DBF ({@link #doImport}), lalu muat seluruh baris data ke tabel itu satu
 * per satu (satu transaksi per baris); (3) panggil {@link #execute} sebagai titik ekstensi
 * pasca-impor (saat ini kosong).
 *
 * <p>
 * <b>Keamanan (diperbaiki)</b>: SQL DDL ({@code CREATE TABLE}) pada {@link #doImport} tetap
 * dirakit lewat konkatenasi string (identifier SQL — nama tabel/kolom — tidak bisa diikat
 * sebagai parameter bind), namun nama tabel (diturunkan dari nama berkas yang diunggah) dan
 * nama kolom (diambil dari nama field DBF sumber) kini divalidasi lewat
 * {@link #sanitizeIdentifier(String, String)} — hanya huruf/angka/garis bawah yang diterima,
 * selain itu {@link IllegalArgumentException} dilempar dan impor berkas tersebut gagal. Nilai
 * data pada {@code INSERT} tidak lagi disisipkan sebagai literal string (sebelumnya hanya
 * disaring dengan menghapus tanda kutip tunggal); kini diikat lewat parameter posisi
 * ({@code ?}) memakai {@code SQLQuery.setParameter(int, Object)}. Sebelumnya kedua celah ini
 * berpotensi SQL/DDL injection bila nama berkas/nama field/isi berkas DBF yang diimpor tidak
 * sepenuhnya tepercaya.
 * </p>
 */
public class ImportFromRKAKLHelper {

	/** Separator baris platform, dipakai saat menyusun string SQL multi-baris agar mudah dibaca di log. */
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
			throw new IllegalArgumentException("Impor RKAKL ditolak: " + konteks
					+ " mengandung karakter yang tidak diperbolehkan (hanya huruf, angka, dan garis bawah diizinkan): "
					+ name);
		}
		return name;
	}

	/** Titik masuk CLI untuk pengujian manual: membaca berkas resource {@code mahasiswa.sql} lewat {@link #read(String)} dan mencetaknya. */
	public static void main(String[] argv) throws IOException {
		read("mahasiswa.sql");
	}

	/**
	 * Menulis {@code data} (baris pertama dipakai sebagai header/nama kolom, sisanya sebagai
	 * isi) ke berkas DBF baru di {@code Common.REAL_PATH + "/tmp/" + name + ".KEU"} — seluruh
	 * kolom dibuat bertipe karakter (255 karakter). Berkas lama dengan nama sama dihapus lebih
	 * dulu bila ada. Kebalikan dari {@link #doImport} (yang membaca DBF, bukan menulis).
	 *
	 * @param data daftar baris, baris ke-0 dipakai sebagai nama kolom
	 * @param name nama dasar berkas (tanpa ekstensi)
	 * @return berkas DBF yang ditulis, atau {@code null} bila {@code data} kosong
	 */
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

	/**
	 * Mengimpor satu berkas DBF ke tabel baru pada skema {@code rab_import}, dengan nama tabel
	 * diambil dari nama berkas (tanpa 4 karakter terakhir, mis. ekstensi {@code .KEU}). Tipe
	 * kolom SQL ditentukan dari tipe field DBF (karakter/numerik/tanggal/float/boolean →
	 * {@code character varying}/{@code numeric}/{@code date}/{@code double precision}/
	 * {@code bool}, tipe lain default ke {@code character varying}). Setelah tabel dibuat,
	 * setiap baris data dibaca dan disisipkan dalam transaksi Hibernate tersendiri per baris
	 * (bukan satu transaksi besar) — kegagalan pada satu baris ditangkap dan ditampilkan lewat
	 * {@link Common#tampilErrorJikaAdmin(Exception)} tanpa menghentikan baris berikutnya.
	 * Progres dilaporkan ke {@code progressmeterChild} per baris dan {@code labelProses} saat
	 * pembuatan tabel gagal. <b>Lihat catatan keamanan pada javadoc kelas mengenai risiko SQL
	 * injection</b> — method ini adalah sumber SQL DDL/DML yang dibangun via konkatenasi string.
	 *
	 * @param file              berkas DBF sumber (biasanya berekstensi {@code .KEU})
	 * @param progressmeter     tidak dipakai langsung di method ini (parameter diteruskan
	 *                          untuk konsistensi signature dengan pemanggil)
	 * @param progressmeterChild indikator progres baris yang diimpor dari berkas ini
	 * @param labelProses       label status, diisi pesan galat bila pembuatan tabel gagal
	 */
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
				String tableName = sanitizeIdentifier(file.getName().substring(0,
						file.getName().length() - 4), "nama tabel (dari nama berkas)");
				String sqlCreateTable = "CREATE TABLE rab_import.\""
						+ tableName + "\" ( " + NL;
				String filed = "";
				for (int i = 0; i < reader.getFieldCount(); i++) {
					DBFField field = reader.getField(i);
					String fieldName = sanitizeIdentifier(field.getName(), "nama kolom (dari field DBF)");

					// field.getDataType()
					if (field.getDataType() == DBFField.FIELD_TYPE_C)
						filed += filed.equals("") ? "\"" + fieldName
								+ "\" character varying("
								+ field.getFieldLength() + ")" : ",\""
								+ fieldName + "\" character varying("
								+ field.getFieldLength() + ")";
					else if (field.getDataType() == DBFField.FIELD_TYPE_N
							|| field.getDataType() == DBFField.FIELD_TYPE_M)
						filed += filed.equals("") ? "\"" + fieldName
								+ "\" numeric" : ",\"" + fieldName
								+ "\" numeric";
					else if (field.getDataType() == DBFField.FIELD_TYPE_D)
						filed += filed.equals("") ? "\"" + fieldName
								+ "\" date" : ",\"" + fieldName
								+ "\" date";
					else if (field.getDataType() == DBFField.FIELD_TYPE_F)
						filed += filed.equals("") ? "\"" + fieldName
								+ "\" double precision" : ",\""
								+ fieldName + "\" double precision";
					else if (field.getDataType() == DBFField.FIELD_TYPE_L)
						filed += filed.equals("") ? "\"" + fieldName
								+ "\" bool" : ",\"" + fieldName
								+ "\" bool";
					else {
						filed += filed.equals("") ? "\"" + fieldName
								+ "\" character varying("
								+ field.getFieldLength() + ")" : ",\""
								+ fieldName + "\" character varying("
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

					String tableName = sanitizeIdentifier(file.getName().substring(0, file.getName()
							.length() - 4), "nama tabel (dari nama berkas)");
					String sqlInsert = "INSERT INTO rab_import.\""
							+ tableName + "\" VALUES ";

					sqlInsert += "(" + NL;
					List<Object> insertParams = new ArrayList<Object>();
					for (int i = 0; i < reader.getFieldCount(); i++) {
						DBFField field = reader.getField(i);

						if (rowobj[i] == null
								|| rowobj[i].toString().trim().equals("null")) {
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
						} else {
							if (!rowobj[i].toString().trim().equals("")) {
								sqlInsert += "?";
								insertParams.add(rowobj[i]);
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
					org.hibernate.SQLQuery insertQuery = session.createSQLQuery(sqlInsert);
					for (int p = 0; p < insertParams.size(); p++) {
						insertQuery.setParameter(p, insertParams.get(p));
					}
					insertQuery.executeUpdate();
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

	/**
	 * Titik masuk utama proses impor RKAKL: menghapus lalu membuat ulang skema
	 * {@code rab_import} (data impor sebelumnya, bila ada, akan HILANG total — ini bukan impor
	 * inkremental), lalu memproses setiap berkas berekstensi {@code .KEU} pada folder
	 * {@code path} lewat {@link #doImport} (berkas yang namanya mengandung {@code "log"} atau
	 * {@code "t_cek"} dilewati). Setelah semua berkas diproses, memanggil {@link #execute}
	 * sebagai titik ekstensi pasca-impor. Kegagalan drop/create skema dicatat ke audit tetapi
	 * tidak menghentikan proses (percobaan create schema tetap dijalankan).
	 *
	 * @param path              folder berisi berkas-berkas {@code .KEU} sumber
	 * @param progressmeter     indikator progres keseluruhan (per berkas)
	 * @param progressmeterChild indikator progres per baris dalam satu berkas, diteruskan ke {@link #doImport}
	 * @param labelProses       label status, diperbarui dengan nama berkas yang sedang diproses
	 */
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

	/**
	 * Membaca seluruh isi berkas resource classpath {@code /ais/action/master/helper/impor/<name>}
	 * (encoding UTF-8) sebagai satu string, baris demi baris. Dipakai untuk keperluan
	 * pengujian/diagnostik manual (lihat {@link #main(String[])}), bukan bagian dari alur impor
	 * produksi ({@link #importData}).
	 *
	 * @param name nama berkas resource relatif terhadap paket ini
	 * @return isi berkas sebagai satu string
	 */
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

	/**
	 * Titik ekstensi pasca-impor, dipanggil oleh {@link #importData} setelah seluruh berkas
	 * DBF selesai dimuat ke skema {@code rab_import}. Saat ini tidak berisi logika apa pun
	 * (hanya membuka lalu menutup sesi Hibernate) — kemungkinan dimaksudkan sebagai tempat
	 * menambahkan langkah transformasi/pemindahan data lanjutan dari {@code rab_import} ke
	 * tabel produksi, yang belum diimplementasikan.
	 */
	public static void execute(Label labelProses) throws Exception {
		Session session = HibernateUtil.currentNativeSession();

		
		HibernateUtil.closeSession();

	}

}
