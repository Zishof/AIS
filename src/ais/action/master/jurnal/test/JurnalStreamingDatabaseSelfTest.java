package ais.action.master.jurnal.test;

import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.database.hibernate.StreamingHibernateUtil;

/**
 * Gate integrasi database streaming. Penulisan BLOB selalu di-rollback sehingga
 * test tidak meninggalkan baris maupun large object pada clone SIT/UAT.
 */
public final class JurnalStreamingDatabaseSelfTest {
    private JurnalStreamingDatabaseSelfTest() {}

    public static void main(String[] args) throws Exception {
        System.setProperty("javax.persistence.validation.mode", "none");
        System.setProperty("hibernate.validator.apply_to_ddl", "false");
        String expected = System.getenv("AIS_JURNAL_STREAMING_DB_NAME");
        if (expected == null || expected.trim().length() == 0
                || "streaming_ais".equalsIgnoreCase(expected.trim())) {
            throw new IllegalStateException(
                    "Test wajib diarahkan ke clone streaming SIT/UAT, bukan baseline streaming_ais.");
        }

        StreamingHibernateUtil util = StreamingHibernateUtil.getInstance();
        Session session = null;
        Transaction transaction = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            session = util.currentSession();
            Connection connection = session.connection();
            statement = connection.prepareStatement("select current_database(), count(*) from public.lampiran_lain");
            result = statement.executeQuery();
            if (!result.next() || !expected.equals(result.getString(1))) {
                throw new IllegalStateException("Database streaming target tidak sesuai environment.");
            }
            long before = result.getLong(2);
            result.close();
            statement.close();
            result = null;
            statement = null;

            transaction = session.beginTransaction();
            byte[] payload = "ais-jurnal-streaming-self-test".getBytes(Charset.forName("UTF-8"));
            statement = connection.prepareStatement(
                    "insert into public.lampiran_lain "
                    + "(ref,jenis,nama,keterangan,olehid,oleh,tanggal_dirubah,foto) "
                    + "values (?,?,?,?,?,?,current_timestamp,lo_from_bytea(0,?)) returning id,foto");
            statement.setLong(1, -91919L);
            statement.setString(2, "JURNAL_REPO_BITSTREAM");
            statement.setString(3, "jurnal-streaming-self-test.txt");
            statement.setString(4, "rollback-only");
            statement.setString(5, "SELF_TEST");
            statement.setString(6, "SELF_TEST");
            statement.setBytes(7, payload);
            result = statement.executeQuery();
            if (!result.next() || result.getLong(1) < 1L || result.getLong(2) < 1L) {
                throw new IllegalStateException("Round-trip BLOB lampiran_lain gagal.");
            }
            long oid = result.getLong(2);
            result.close();
            statement.close();
            result = null;
            statement = connection.prepareStatement("select octet_length(lo_get(?))");
            statement.setLong(1, oid);
            result = statement.executeQuery();
            if (!result.next() || result.getLong(1) != payload.length) {
                throw new IllegalStateException("Isi BLOB lampiran_lain tidak sesuai.");
            }
            result.close();
            statement.close();
            result = null;
            statement = null;
            transaction.rollback();
            transaction = null;

            statement = connection.prepareStatement("select count(*) from public.lampiran_lain");
            result = statement.executeQuery();
            if (!result.next() || result.getLong(1) != before) {
                throw new IllegalStateException("Rollback test meninggalkan data pada lampiran_lain.");
            }
            System.out.println("JurnalStreamingDatabaseSelfTest OK database=clone rows=" + before);
        } finally {
            if (transaction != null && transaction.isActive()) transaction.rollback();
            if (result != null) try { result.close(); } catch (Exception ignored) {}
            if (statement != null) try { statement.close(); } catch (Exception ignored) {}
            try { util.closeSession(); } catch (Exception ignored) {}
        }
        System.exit(0);
    }
}
