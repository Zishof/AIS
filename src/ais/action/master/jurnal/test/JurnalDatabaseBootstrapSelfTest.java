package ais.action.master.jurnal.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.hibernate.Session;
import ais.database.hibernate.HibernateUtil;

/** Read-only bootstrap/schema contract test; target DB must be supplied by AIS_JURNAL_DB_* env. */
public final class JurnalDatabaseBootstrapSelfTest {
    private JurnalDatabaseBootstrapSelfTest() {}

    public static void main(String[] args) throws Exception {
        System.setProperty("javax.persistence.validation.mode","none");
        System.setProperty("hibernate.validator.apply_to_ddl","false");
        String expected = System.getenv("AIS_JURNAL_DB_NAME");
        if (expected == null || expected.trim().length() == 0 || "ais".equalsIgnoreCase(expected.trim()))
            throw new IllegalStateException("Test wajib diarahkan ke clone SIT/UAT, bukan database baseline ais.");
        Session session = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            session = HibernateUtil.openSession();
            Connection connection = session.connection();
            statement = connection.prepareStatement(
                "select current_database(), count(*) from information_schema.tables " +
                "where table_schema='penelitiandanpengabdian' and table_name in " +
                "('template_email_jurnal','langganan_jurnal','undangan_peran_jurnal'," +
                "'peserta_diskusi_jurnal','penugasan_tahap_jurnal','penugasan_reviewer_jurnal'," +
                "'agregat_penggunaan_jurnal','rentang_ip_langganan_jurnal','import_sumber_ojs'," +
                "'import_job_ojs','import_checkpoint_ojs','import_mapping_ojs')");
            result = statement.executeQuery();
            if (!result.next()) throw new IllegalStateException("Schema verification tidak menghasilkan baris.");
            String actual = result.getString(1);
            int tables = result.getInt(2);
            if (!expected.equals(actual)) throw new IllegalStateException("Database target tidak sesuai environment.");
            if (tables != 12) throw new IllegalStateException("Tabel jurnal fisik bukan 12: " + tables);
            System.out.println("JurnalDatabaseBootstrapSelfTest OK database=clone tables=" + tables);
        } finally {
            if (result != null) try { result.close(); } catch (Exception ignored) {}
            if (statement != null) try { statement.close(); } catch (Exception ignored) {}
            HibernateUtil.closeSessionQuietly(session);
        }
        System.exit(0);
    }
}
