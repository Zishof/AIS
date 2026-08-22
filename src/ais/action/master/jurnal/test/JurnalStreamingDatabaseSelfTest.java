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
            statement = connection.prepareStatement("select current_database(), count(*) from public.lampiran_jurnal");
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
                    "insert into public.lampiran_jurnal "
                    + "(repo_bitstream_id,original_file_name,declared_mime_type,detected_mime_type,"
                    + "declared_size,actual_size,checksum_sha256,journal_stage,file_version,storage_state,"
                    + "scan_state,quarantine_state,idempotency_key,created_by,updated_by,created_at,updated_at,file_content) "
                    + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,current_timestamp,current_timestamp,lo_from_bytea(0,?)) "
                    + "returning id,file_content");
            statement.setLong(1, -91919L);
            statement.setString(2, "jurnal-streaming-self-test.txt");
            statement.setString(3, "text/plain");
            statement.setString(4, "text/plain");
            statement.setLong(5, payload.length);
            statement.setLong(6, payload.length);
            statement.setString(7, "8f0a95b04670bec92a073965f2a9b86e7be9479648f8d8b7bdc9c5f095283a4f");
            statement.setString(8, "SUBMISSION");
            statement.setLong(9, 1L);
            statement.setString(10, "AVAILABLE");
            statement.setString(11, "NOT_CONFIGURED");
            statement.setString(12, "RELEASED_BY_POLICY");
            statement.setString(13, "SELF_TEST:-91919");
            statement.setString(14, "SELF_TEST");
            statement.setString(15, "SELF_TEST");
            statement.setBytes(16, payload);
            result = statement.executeQuery();
            if (!result.next() || result.getLong(1) < 1L || result.getLong(2) < 1L) {
                throw new IllegalStateException("Round-trip BLOB lampiran_jurnal gagal.");
            }
            long oid = result.getLong(2);
            result.close();
            statement.close();
            result = null;
            statement = connection.prepareStatement("select octet_length(lo_get(?))");
            statement.setLong(1, oid);
            result = statement.executeQuery();
            if (!result.next() || result.getLong(1) != payload.length) {
                throw new IllegalStateException("Isi BLOB lampiran_jurnal tidak sesuai.");
            }
            result.close();
            statement.close();
            result = null;
            statement = null;
            transaction.rollback();
            transaction = null;

            statement = connection.prepareStatement("select count(*) from public.lampiran_jurnal");
            result = statement.executeQuery();
            if (!result.next() || result.getLong(1) != before) {
                throw new IllegalStateException("Rollback test meninggalkan data pada lampiran_jurnal.");
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
