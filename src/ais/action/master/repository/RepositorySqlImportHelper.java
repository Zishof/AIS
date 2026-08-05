package ais.action.master.repository;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.hibernate.Session;

import ais.database.hibernate.HibernateUtil;

public class RepositorySqlImportHelper {

    public interface ProgressListener {
        void onProgress(int percent, String message);
    }

    public static class Result {
        public String schema;
        public File sqlFile;
        public int restoredStatements;
        public int skippedStatements;
        public int failedStatements;
        public int importedItems;
        public int importedMetadata;
        public int importedBitstreams;
        public List<String> messages = new ArrayList<String>();
    }

    public Result importSql(File sqlFile, ProgressListener progressListener) throws Exception {
        if (sqlFile == null || !sqlFile.exists() || !sqlFile.isFile()) {
            throw new IllegalArgumentException("File SQL tidak ditemukan.");
        }
        if (!sqlFile.getName().toLowerCase(Locale.ENGLISH).endsWith(".sql")) {
            throw new IllegalArgumentException("File yang di-upload harus berformat .sql.");
        }

        Result result = new Result();
        result.sqlFile = sqlFile;
        result.schema = "repository_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        update(progressListener, 3, "Validasi file SQL repository selesai.");
        restoreSql(result, progressListener);
        migrateToRepository(result, progressListener);
        update(progressListener, 100, "Import repository selesai.");
        return result;
    }

    private void restoreSql(Result result, ProgressListener progressListener) throws Exception {
        update(progressListener, 8, "Membuat schema temporary " + result.schema + ".");
        Session session = HibernateUtil.currentSession();
        executeSql(session, "create schema if not exists " + qident(result.schema));
        executeSql(session, "set standard_conforming_strings = off");

        long length = result.sqlFile.length();
        long read = 0L;
        StringBuilder statement = new StringBuilder();
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(result.sqlFile));
        boolean inString = false;
        boolean escaped = false;
        int b;
        try {
            while ((b = in.read()) != -1) {
                read++;
                char c = (char) b;
                statement.append(c);
                if (inString) {
                    if (escaped) {
                        escaped = false;
                    } else if (c == '\\') {
                        escaped = true;
                    } else if (c == '\'') {
                        inString = false;
                    }
                } else if (c == '\'') {
                    inString = true;
                } else if (c == ';') {
                    processMysqlStatement(session, statement.toString(), result);
                    statement.setLength(0);
                    if ((result.restoredStatements + result.failedStatements + result.skippedStatements) % 100 == 0) {
                        int percent = 10 + (int) Math.min(45L, (read * 45L) / Math.max(1L, length));
                        update(progressListener, percent, "Restore SQL staging: " + result.restoredStatements
                                + " statement berhasil, gagal " + result.failedStatements + ".");
                    }
                }
            }
            if (statement.length() > 0) {
                processMysqlStatement(session, statement.toString(), result);
            }
        } finally {
            IOUtils.closeQuietly(in);
        }
        update(progressListener, 58, "Restore SQL selesai. Berhasil " + result.restoredStatements + ", dilewati "
                + result.skippedStatements + ", gagal " + result.failedStatements + ".");
    }

    private void processMysqlStatement(Session session, String raw, Result result) {
        String statement = stripLeadingMysqlComments(raw == null ? "" : raw.trim());
        String upper = statement.toUpperCase(Locale.ENGLISH);
        if (statement.length() == 0 || statement.startsWith("--") || statement.startsWith("/*!") || upper.startsWith("SET ")
                || upper.startsWith("START TRANSACTION") || upper.startsWith("COMMIT") || upper.startsWith("LOCK TABLES")
                || upper.startsWith("UNLOCK TABLES") || upper.startsWith("DROP TABLE") || upper.startsWith("ALTER TABLE")) {
            result.skippedStatements++;
            return;
        }
        String converted = null;
        if (upper.startsWith("CREATE TABLE")) {
            converted = convertCreateTable(statement, result.schema);
        } else if (upper.startsWith("INSERT INTO")) {
            converted = convertInsert(statement, result.schema);
        } else {
            result.skippedStatements++;
            return;
        }
        if (converted == null || converted.trim().length() == 0) {
            result.skippedStatements++;
            return;
        }
        try {
            executeSql(session, converted);
            result.restoredStatements++;
        } catch (Exception e) {
            result.failedStatements++;
            if (result.messages.size() < 25) {
                result.messages.add("Gagal restore statement: " + shortMessage(e));
            }
        }
    }

    private String convertCreateTable(String statement, String schema) {
        String table = extractMysqlTableName(statement);
        if (table == null) {
            return null;
        }
        int open = statement.indexOf('(');
        int close = statement.lastIndexOf(')');
        if (open < 0 || close < open) {
            return null;
        }
        String body = statement.substring(open + 1, close);
        List<String> columns = splitTopLevel(body);
        StringBuilder sql = new StringBuilder();
        sql.append("create table if not exists ").append(qident(schema)).append(".").append(qident(table)).append(" (");
        boolean first = true;
        for (int i = 0; i < columns.size(); i++) {
            String col = columns.get(i).trim();
            if (!col.startsWith("`")) {
                continue;
            }
            int end = col.indexOf('`', 1);
            if (end < 0) {
                continue;
            }
            String name = col.substring(1, end);
            String rest = col.substring(end + 1).trim();
            if (!first) {
                sql.append(", ");
            }
            sql.append(qident(name)).append(" ").append(mapMysqlType(rest));
            first = false;
        }
        sql.append(")");
        return first ? null : sql.toString();
    }

    private String stripLeadingMysqlComments(String statement) {
        String result = statement == null ? "" : statement.trim();
        boolean changed = true;
        while (changed) {
            changed = false;
            if (result.startsWith("--")) {
                int next = result.indexOf('\n');
                result = next < 0 ? "" : result.substring(next + 1).trim();
                changed = true;
            } else if (result.startsWith("#")) {
                int next = result.indexOf('\n');
                result = next < 0 ? "" : result.substring(next + 1).trim();
                changed = true;
            }
        }
        return result;
    }

    private String convertInsert(String statement, String schema) {
        String table = extractMysqlTableName(statement);
        if (table == null) {
            return null;
        }
        String sql = statement.replaceFirst("(?is)INSERT\\s+INTO\\s+`?" + table + "`?",
                "INSERT INTO " + qident(schema) + "." + qident(table));
        sql = sql.replace('`', '"');
        sql = sql.replaceAll("(?i)_binary\\s+'", "'");
        return sql;
    }

    private String extractMysqlTableName(String statement) {
        int tick = statement.indexOf('`');
        if (tick >= 0) {
            int end = statement.indexOf('`', tick + 1);
            return end > tick ? statement.substring(tick + 1, end) : null;
        }
        String[] parts = statement.split("\\s+");
        return parts.length >= 3 ? parts[2] : null;
    }

    private String mapMysqlType(String raw) {
        String t = raw.toLowerCase(Locale.ENGLISH);
        t = t.replaceAll("(?i)character\\s+set\\s+\\S+", " ");
        t = t.replaceAll("(?i)collate\\s+\\S+", " ");
        t = t.replaceAll("(?i)unsigned", " ");
        t = t.replaceAll("(?i)zerofill", " ");
        t = t.replaceAll("(?i)not\\s+null", " ");
        t = t.replaceAll("(?i)null", " ");
        t = t.replaceAll("(?i)auto_increment", " ");
        t = t.replaceAll("(?i)on\\s+update\\s+current_timestamp", " ");
        t = t.replaceAll("(?i)comment\\s+'([^'\\\\]|\\\\.)*'", " ");
        t = t.replaceAll("(?i)default\\s+current_timestamp", " ");
        t = t.replaceAll("(?i)default\\s+'0000-00-00( 00:00:00)?'", " ");
        t = t.replaceAll("(?i)default\\s+[^\\s,]+", " ").trim();
        if (t.startsWith("bigint")) {
            return "bigint";
        }
        if (t.startsWith("int") || t.startsWith("mediumint")) {
            return "integer";
        }
        if (t.startsWith("smallint") || t.startsWith("tinyint")) {
            return "smallint";
        }
        if (t.startsWith("datetime") || t.startsWith("timestamp")) {
            return "timestamp";
        }
        if (t.startsWith("date")) {
            return "date";
        }
        if (t.startsWith("decimal") || t.startsWith("double") || t.startsWith("float")) {
            return "numeric";
        }
        if (t.startsWith("varchar")) {
            int end = t.indexOf(')');
            return end > 0 ? t.substring(0, end + 1) : "varchar(255)";
        }
        if (t.startsWith("char")) {
            int end = t.indexOf(')');
            return end > 0 ? t.substring(0, end + 1) : "char(1)";
        }
        if (t.startsWith("blob") || t.startsWith("longblob") || t.startsWith("mediumblob")) {
            return "bytea";
        }
        return "text";
    }

    private List<String> splitTopLevel(String body) {
        List<String> result = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        int level = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (inString) {
                current.append(c);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '\'') {
                    inString = false;
                }
                continue;
            }
            if (c == '\'') {
                inString = true;
                current.append(c);
                continue;
            }
            if (c == '(') {
                level++;
            } else if (c == ')') {
                level--;
            }
            if (c == ',' && level == 0) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }
        return result;
    }

    private void migrateToRepository(Result result, ProgressListener progressListener) {
        Session session = HibernateUtil.currentSession();
        update(progressListener, 62, "Memeriksa tabel staging repository.");
        if (!tableExists(session, result.schema, "biblio")) {
            throw new IllegalArgumentException("File SQL tidak berisi tabel biblio repository.");
        }

        update(progressListener, 66, "Menyiapkan collection repository.");
        executeSql(session, "insert into public.repo_collection (kode, nama, deskripsi, tipe, source_system, sort_order, aktif, tanggal_dirubah) "
                + "select 'REPOSITORY_IMPORT', 'Repository Import', 'Data hasil import SQL repository', 'COLLECTION', 'REPOSITORY_SQL', 0, true, now() "
                + "where not exists (select 1 from public.repo_collection where kode='REPOSITORY_IMPORT')");
        Long collectionId = numberToLong((Number) session.createSQLQuery(
                "select id from public.repo_collection where kode='REPOSITORY_IMPORT' order by id limit 1").uniqueResult());
        if (collectionId == null) {
            throw new IllegalStateException("Collection repository import tidak dapat dibuat.");
        }

        update(progressListener, 72, "Memigrasikan item repository.");
        result.importedItems = executeCountSql(session, buildUpdateItemsSql(result.schema, collectionId));
        result.importedItems += executeCountSql(session, buildInsertItemsSql(result.schema, collectionId));

        update(progressListener, 82, "Memigrasikan metadata Dublin Core.");
        result.importedMetadata = insertMetadata(result.schema);

        update(progressListener, 91, "Memigrasikan file/bitstream repository.");
        result.importedBitstreams = insertBitstreams(result.schema);
    }

    private String buildUpdateItemsSql(String schema, Long collectionId) {
        return "update public.repo_item ri set "
                + "collection_id=" + collectionId + ", "
                + "is_withdrawn=(coalesce(b.opac_hide,0)<>0), "
                + "source_label='Repository SQL', "
                + "title=left(coalesce(nullif(b.title,''),'Untitled'), 2000), "
                + "abstract_text=coalesce(nullif(b.spec_detail_info,''), nullif(b.notes,''), ''), "
                + "authors=left(coalesce(nullif(a.authors,''), nullif(b.sor,''), ''), 2000), "
                + "subjects=left(coalesce(nullif(t.subjects,''), nullif(b.classification,''), ''), 2000), "
                + "publisher=left(coalesce(p.publisher_name,''),255), "
                + "language=left(coalesce(l.language_name, b.language_id, 'id'),30), "
                + "document_type=left(coalesce(it.item_type_name, g.gmd_name, 'Repository'),80), "
                + "access_policy='OPEN_ACCESS', sync_status='DRAFT', "
                + "sync_message='Imported from schema " + esc(schema) + "', "
                + "submitted_at=b.input_date, issued_at=case when b.publish_year ~ '^[0-9]{4}$' then to_date(b.publish_year || '-01-01','YYYY-MM-DD') else null end, "
                + "tanggal_dirubah=now(), aktif=true "
                + fromRepositoryData(schema)
                + " where ri.oai_identifier='repository:biblio:' || b.biblio_id::text";
    }

    private String buildInsertItemsSql(String schema, Long collectionId) {
        return "insert into public.repo_item (collection_id, oai_identifier, is_withdrawn, source_class, source_id, source_label, title, abstract_text, authors, subjects, publisher, language, document_type, access_policy, sync_status, sync_message, submitted_at, issued_at, tanggal_dirubah, aktif) "
                + "select " + collectionId + ", 'repository:biblio:' || b.biblio_id::text, (coalesce(b.opac_hide,0)<>0), "
                + "'repository.biblio', b.biblio_id, 'Repository SQL', "
                + "left(coalesce(nullif(b.title,''),'Untitled'), 2000), "
                + "coalesce(nullif(b.spec_detail_info,''), nullif(b.notes,''), ''), "
                + "left(coalesce(nullif(a.authors,''), nullif(b.sor,''), ''), 2000), "
                + "left(coalesce(nullif(t.subjects,''), nullif(b.classification,''), ''), 2000), "
                + "left(coalesce(p.publisher_name,''),255), "
                + "left(coalesce(l.language_name, b.language_id, 'id'),30), "
                + "left(coalesce(it.item_type_name, g.gmd_name, 'Repository'),80), "
                + "'OPEN_ACCESS', 'DRAFT', 'Imported from schema " + esc(schema) + "', "
                + "b.input_date, case when b.publish_year ~ '^[0-9]{4}$' then to_date(b.publish_year || '-01-01','YYYY-MM-DD') else null end, now(), true "
                + fromRepositoryData(schema)
                + " where not exists (select 1 from public.repo_item x where x.oai_identifier='repository:biblio:' || b.biblio_id::text)";
    }

    private String fromRepositoryData(String schema) {
        return " from " + qs(schema, "biblio") + " b "
                + "left join " + qs(schema, "mst_publisher") + " p on p.publisher_id=b.publisher_id "
                + "left join " + qs(schema, "mst_language") + " l on l.language_id=b.language_id "
                + "left join " + qs(schema, "mst_item_type") + " it on it.item_type_id=b.item_type_id "
                + "left join " + qs(schema, "mst_gmd") + " g on g.gmd_id=b.gmd_id "
                + "left join (select ba.biblio_id, string_agg(ma.author_name, '; ' order by ba.level, ma.author_name) authors "
                + "from " + qs(schema, "biblio_author") + " ba join " + qs(schema, "mst_author") + " ma on ma.author_id=ba.author_id group by ba.biblio_id) a on a.biblio_id=b.biblio_id "
                + "left join (select bt.biblio_id, string_agg(mt.topic, '; ' order by bt.level, mt.topic) subjects "
                + "from " + qs(schema, "biblio_topic") + " bt join " + qs(schema, "mst_topic") + " mt on mt.topic_id=bt.topic_id group by bt.biblio_id) t on t.biblio_id=b.biblio_id ";
    }

    private int insertMetadata(String schema) {
        String base = " from public.repo_item ri join " + qs(schema, "biblio") + " b on ri.oai_identifier='repository:biblio:' || b.biblio_id::text "
                + "left join " + qs(schema, "mst_publisher") + " p on p.publisher_id=b.publisher_id "
                + "left join " + qs(schema, "mst_language") + " l on l.language_id=b.language_id ";
        int total = 0;
        total += insertMetadataField(schema, "dc.title", "b.title", base);
        total += insertMetadataField(schema, "dc.creator", "coalesce(nullif((select string_agg(ma.author_name, '; ' order by ba.level, ma.author_name) from " + qs(schema, "biblio_author") + " ba join " + qs(schema, "mst_author") + " ma on ma.author_id=ba.author_id where ba.biblio_id=b.biblio_id),''), b.sor)", base);
        total += insertMetadataField(schema, "dc.subject", "coalesce(nullif((select string_agg(mt.topic, '; ' order by bt.level, mt.topic) from " + qs(schema, "biblio_topic") + " bt join " + qs(schema, "mst_topic") + " mt on mt.topic_id=bt.topic_id where bt.biblio_id=b.biblio_id),''), b.classification)", base);
        total += insertMetadataField(schema, "dc.description.abstract", "coalesce(nullif(b.spec_detail_info,''), b.notes)", base);
        total += insertMetadataField(schema, "dc.publisher", "p.publisher_name", base);
        total += insertMetadataField(schema, "dc.identifier.isbn", "b.isbn_issn", base);
        total += insertMetadataField(schema, "dc.date.issued", "b.publish_year", base);
        total += insertMetadataField(schema, "dc.language.iso", "coalesce(b.language_id, l.language_name)", base);
        total += insertMetadataField(schema, "dc.identifier.callnumber", "b.call_number", base);
        total += insertMetadataField(schema, "dc.identifier.other", "b.unique_id", base);
        return total;
    }

    private int insertMetadataField(String schema, String field, String valueExpression, String baseFrom) {
        String value = "nullif(trim((" + valueExpression + ")::text),'')";
        String sql = "insert into public.repo_item_metadata (item_id, metadata_field, metadata_value, language, place, confidence, tanggal_dirubah, aktif) "
                + "select ri.id, '" + esc(field) + "', left(" + value + ", 8000), null, 0, -1, now(), true "
                + baseFrom
                + " where " + value + " is not null "
                + "and not exists (select 1 from public.repo_item_metadata m where m.item_id=ri.id and m.metadata_field='"
                + esc(field) + "' and m.metadata_value=left(" + value + ", 8000))";
        return executeCountSql(HibernateUtil.currentSession(), sql);
    }

    private int insertBitstreams(String schema) {
        if (!tableExists(HibernateUtil.currentSession(), schema, "biblio_attachment")
                || !tableExists(HibernateUtil.currentSession(), schema, "files")) {
            return 0;
        }
        String path = "left(coalesce(nullif(f.file_url,''), nullif(f.file_dir || '/' || f.file_name, '/'), f.file_name), 500)";
        String sql = "insert into public.repo_bitstream (item_id, nama_file, mime_type, path_sistem, description, bundle_name, access_policy, source_class, source_id, primary_file, tanggal_dirubah, aktif) "
                + "select ri.id, left(coalesce(nullif(f.file_name,''), 'file-' || f.file_id::text),255), "
                + "left(coalesce(nullif(f.mime_type,''),'application/octet-stream'),100), " + path + ", "
                + "coalesce(nullif(f.file_desc,''), nullif(f.file_title,''), ''), 'ORIGINAL', "
                + "case when ba.access_type='private' then 'PRIVATE' else 'OPEN_ACCESS' end, "
                + "'repository.files', f.file_id, "
                + "not exists (select 1 from public.repo_bitstream pb where pb.item_id=ri.id), now(), true "
                + "from " + qs(schema, "biblio_attachment") + " ba "
                + "join " + qs(schema, "files") + " f on f.file_id=ba.file_id "
                + "join public.repo_item ri on ri.oai_identifier='repository:biblio:' || ba.biblio_id::text "
                + "where nullif(f.file_name,'') is not null "
                + "and not exists (select 1 from public.repo_bitstream x where x.item_id=ri.id and x.source_class='repository.files' and x.source_id=f.file_id)";
        return executeCountSql(HibernateUtil.currentSession(), sql);
    }

    private boolean tableExists(Session session, String schema, String table) {
        Number n = (Number) session
                .createSQLQuery("select count(*) from information_schema.tables where table_schema=:schema and table_name=:table")
                .setParameter("schema", schema).setParameter("table", table).uniqueResult();
        return n != null && n.intValue() > 0;
    }

    private int executeCountSql(Session session, String sql) {
        org.hibernate.Transaction tx = null;
        try {
            if (!session.getTransaction().isActive()) {
                tx = session.beginTransaction();
            }
            int count = session.createSQLQuery(sql).executeUpdate();
            if (tx != null) {
                tx.commit();
            }
            return count;
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    private void executeSql(Session session, String sql) {
        executeCountSql(session, sql);
    }

    private static String qs(String schema, String table) {
        return qident(schema) + "." + qident(table);
    }

    private static String qident(String name) {
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("'", "''");
    }

    private static Long numberToLong(Number n) {
        return n == null ? null : Long.valueOf(n.longValue());
    }

    private static String shortMessage(Exception e) {
        String message = e == null ? "" : e.getMessage();
        if (message == null) {
            message = e.getClass().getName();
        }
        return message.length() > 240 ? message.substring(0, 240) : message;
    }

    private void update(ProgressListener progressListener, int percent, String message) {
        if (progressListener != null) {
            progressListener.onProgress(percent, message);
        }
    }
}
