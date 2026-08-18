package ais.common;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.database.hibernate.HibernateUtil;

/**
 * Memastikan kolom narasi/detail pada virtual_account_bank cukup panjang.
 *
 * Error yang dicegah:
 * org.postgresql.util.PSQLException: ERROR: value too long for type character varying(255)
 *
 * Cara kerja:
 * - Cek information_schema lebih dulu.
 * - Jika kolom masih character varying dengan panjang terbatas, baru ALTER ke text.
 * - Jika sudah text, tidak melakukan apa-apa.
 * - Setiap kolom memakai transaksi terpisah agar satu kegagalan tidak membuat koneksi
 *   berada pada status aborted untuk kolom berikutnya.
 *
 * Kompatibel Java 1.6/1.7 dan PostgreSQL lama.
 */
public final class VirtualAccountBankSchemaFix {

	private static volatile boolean sudahDicek = false;
	private static final Object LOCK = new Object();

	private VirtualAccountBankSchemaFix() {
	}

	public static void initTextColumns() {
		synchronized (LOCK) {
			if (sudahDicek) {
				return;
			}
			sudahDicek = true;
		}

		String[] columns = new String[] { "keterangan", "cicilan", "detailbiaya", "bulanan", "request", "response",
				"link", "notif", "barcode" };

		for (int i = 0; i < columns.length; i++) {
			alterColumnToTextIfNeeded(columns[i]);
		}
	}

	private static void alterColumnToTextIfNeeded(String columnName) {
		if (columnName == null || columnName.trim().length() == 0) {
			return;
		}

		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			if (!isColumnNeedAlter(session, columnName)) {
				return;
			}

			tx = session.beginTransaction();
			session.createSQLQuery("ALTER TABLE virtual_account_bank ALTER COLUMN " + columnName + " TYPE text")
					.executeUpdate();
			tx.commit();
			try {
				System.out.println("VirtualAccountBankSchemaFix: kolom virtual_account_bank." + columnName
						+ " berhasil diubah menjadi text.");
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/VirtualAccountBankSchemaFix.java:70");
			}
		} catch (Exception e) {
			rollbackQuietly(tx);
			try {
				System.out.println("VirtualAccountBankSchemaFix gagal alter kolom " + columnName + ": "
						+ e.getMessage());
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/VirtualAccountBankSchemaFix.java:77");
			}
		} finally {
			closeQuietly(session);
		}
	}

	private static boolean isColumnNeedAlter(Session session, String columnName) {
		try {
			List rows = session.createSQLQuery(
					"select data_type, character_maximum_length from information_schema.columns "
							+ "where table_schema = 'public' and table_name = 'virtual_account_bank' and column_name = :columnName")
					.setString("columnName", columnName).list();

			if (rows == null || rows.isEmpty()) {
				return false;
			}

			Object row = rows.get(0);
			Object dataType = null;
			Object length = null;
			if (row instanceof Object[]) {
				Object[] arr = (Object[]) row;
				dataType = arr.length > 0 ? arr[0] : null;
				length = arr.length > 1 ? arr[1] : null;
			} else {
				dataType = row;
			}

			String type = dataType == null ? "" : dataType.toString().toLowerCase();
			if ("text".equals(type)) {
				return false;
			}
			if (type.indexOf("character varying") >= 0 || type.indexOf("varchar") >= 0) {
				return true;
			}
			if (length instanceof Number && ((Number) length).intValue() > 0) {
				return true;
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	private static void rollbackQuietly(Transaction tx) {
		if (tx == null) {
			return;
		}
		try {
			if (tx.isActive()) {
				tx.rollback();
			}
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/VirtualAccountBankSchemaFix.java:130");
		}
	}

	private static void closeQuietly(Session session) {
		if (session == null) {
			return;
		}
		try {
			if (session.isOpen()) {
				session.clear();
			}
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/VirtualAccountBankSchemaFix.java:142");
		}
		try {
			session.disconnect();
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/VirtualAccountBankSchemaFix.java:146");
		}
		try {
			if (session.isOpen()) {
				session.close();
			}
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/VirtualAccountBankSchemaFix.java:152");
		}
	}
}
