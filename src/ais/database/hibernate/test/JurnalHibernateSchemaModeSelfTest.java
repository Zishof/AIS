package ais.database.hibernate.test;

import org.hibernate.cfg.Configuration;

import ais.database.hibernate.DbCredentialOverride;

/** Verifies journal main/streaming environments force migration-only schema mode. */
public final class JurnalHibernateSchemaModeSelfTest {
    private JurnalHibernateSchemaModeSelfTest() {}

    public static void main(String[] args) {
        require("AIS_JURNAL_DB_NAME");
        require("AIS_JURNAL_DB_USER");
        require("AIS_JURNAL_DB_PASSWORD");
        require("AIS_JURNAL_STREAMING_DB_NAME");
        require("AIS_JURNAL_STREAMING_DB_USER");
        require("AIS_JURNAL_STREAMING_DB_PASSWORD");
        Configuration main = new Configuration();
        main.setProperty("hibernate.hbm2ddl.auto", "update");
        DbCredentialOverride.terapkan(main, "utama");
        check("none".equals(main.getProperty("hibernate.hbm2ddl.auto")), "Main jurnal tidak memaksa hbm2ddl none.");
        check("false".equals(main.getProperty("hibernate.use_sql_comments")), "SQL comments main jurnal harus nonaktif.");
        Configuration streaming = new Configuration();
        streaming.setProperty("hibernate.hbm2ddl.auto", "update");
        DbCredentialOverride.terapkan(streaming, "streaming");
        String expected = "true".equalsIgnoreCase(System.getenv("AIS_JURNAL_STREAMING_SCHEMA_UPDATE")) ? "update" : "none";
        check(expected.equals(streaming.getProperty("hibernate.hbm2ddl.auto")), "Mode schema streaming jurnal tidak sesuai opt-in.");
        check("false".equals(streaming.getProperty("hibernate.use_sql_comments")), "SQL comments streaming jurnal harus nonaktif.");
        System.out.println("JurnalHibernateSchemaModeSelfTest OK main=none streaming=" + expected + " sql-comments=false");
    }

    private static void require(String key) {
        String value = System.getenv(key);
        if (value == null || value.length() == 0) throw new IllegalStateException("Environment wajib: " + key);
    }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
