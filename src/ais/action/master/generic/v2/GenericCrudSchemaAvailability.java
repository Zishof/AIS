package ais.action.master.generic.v2;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import org.hibernate.Session;

/** Pemeriksaan tabel tanpa menjalankan SELECT terhadap relasi yang belum ada. */
final class GenericCrudSchemaAvailability {

    private GenericCrudSchemaAvailability() { }

    static boolean hasTable(Session session, String tableName) {
        if (session == null || tableName == null || !tableName.matches("[a-z0-9_]+")) return false;
        ResultSet tables = null;
        try {
            Connection connection = session.connection();
            if (connection == null) return false;
            DatabaseMetaData metadata = connection.getMetaData();
            tables = metadata.getTables(connection.getCatalog(), null, tableName, new String[] { "TABLE" });
            if (tables.next()) return true;
            close(tables); tables = metadata.getTables(connection.getCatalog(), "public", tableName,
                    new String[] { "TABLE" });
            return tables.next();
        } catch (Exception unavailable) {
            return false;
        } finally {
            close(tables);
        }
    }

    static void requireTable(Session session, String tableName) throws GenericCrudException {
        if (!hasTable(session, tableName)) {
            throw new GenericCrudException(503, "MIGRATION_REQUIRED",
                    "Jalankan migration Generic CRUD 001; tabel " + tableName + " belum tersedia.");
        }
    }

    private static void close(ResultSet value) {
        try { if (value != null) value.close(); } catch (Exception ignored) { }
    }
}
