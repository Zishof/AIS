package ais.common;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.database.hibernate.HibernateUtil;

/**
 * Memastikan kolom snapshot hasil nilai tersedia pada tabel utama dan audit.
 * Proses idempoten: kolom hanya ditambahkan bila belum ada.
 */
public final class NilaiKunciSchemaFix {

	private NilaiKunciSchemaFix() {
	}

	public static void initKolomSnapshot() {
		String[][] kolom = new String[][] {
				{ "total_nilai_kunci", "double precision" },
				{ "nilai_huruf_kunci", "varchar(2)" },
				{ "nilai_ip_kunci", "double precision" },
				{ "lulus_kunci", "boolean" },
				{ "total_nilai_sementara_kunci", "double precision" },
				{ "nilai_huruf_sementara_kunci", "varchar(2)" },
				{ "nilai_ip_sementara_kunci", "double precision" } };

		for (int i = 0; i < kolom.length; i++) {
			tambahJikaBelumAda("public", "detailperkuliahan", kolom[i][0], kolom[i][1]);
			tambahJikaBelumAda("new_audit", "detailperkuliahan__audit", kolom[i][0], kolom[i][1]);
		}
	}

	private static void tambahJikaBelumAda(String schema, String tabel, String kolom, String tipe) {
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			List hasil = session.createSQLQuery("select column_name from information_schema.columns "
					+ "where table_schema=:schema and table_name=:tabel and column_name=:kolom")
					.setParameter("schema", schema).setParameter("tabel", tabel)
					.setParameter("kolom", kolom).list();
			if (hasil != null && !hasil.isEmpty()) {
				return;
			}

			tx = session.beginTransaction();
			session.createSQLQuery("alter table \"" + schema + "\".\"" + tabel
					+ "\" add column \"" + kolom + "\" " + tipe).executeUpdate();
			tx.commit();
		} catch (Exception e) {
			if (tx != null) {
				try {
					if (tx.isActive()) {
						tx.rollback();
					}
				} catch (Exception rollbackError) {
					ais.common.ErrorAuditUtil.record(rollbackError,
							"NilaiKunciSchemaFix.rollback " + schema + "." + tabel + "." + kolom);
				}
			}
			// Tabel audit dapat belum tersedia pada instalasi lama. Hibernate tetap
			// menangani tabel utama; kegagalan satu target tidak menghentikan startup.
			ais.common.ErrorAuditUtil.record(e,
					"NilaiKunciSchemaFix " + schema + "." + tabel + "." + kolom);
		} finally {
			if (session != null) {
				try {
					if (session.isOpen()) {
						session.clear();
						session.disconnect();
						session.close();
					}
				} catch (Exception closeError) {
					ais.common.ErrorAuditUtil.record(closeError,
							"NilaiKunciSchemaFix.close " + schema + "." + tabel + "." + kolom);
				}
			}
		}
	}
}
