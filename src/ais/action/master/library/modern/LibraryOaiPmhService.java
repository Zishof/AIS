package ais.action.master.library.modern;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Query;
import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Perpustakaan;

/** Minimal OAI-PMH 2.0 provider backed by active AIS bibliographic records. */
public final class LibraryOaiPmhService {
    private static final int PAGE_SIZE = 100;
    private LibraryOaiPmhService() { }

    public static Result handle(HttpServletRequest request) {
        try {
            String verb = text(request.getParameter("verb"), 40);
            if (verb == null) return error("badVerb", "Parameter verb wajib diisi.", request);
            if ("Identify".equals(verb)) return identify(request);
            if ("ListMetadataFormats".equals(verb)) return formats(request);
            if ("GetRecord".equals(verb)) return getRecord(request);
            if ("ListIdentifiers".equals(verb)) return list(request, false);
            if ("ListRecords".equals(verb)) return list(request, true);
            return error("badVerb", "Verb tidak didukung.", request);
        } catch (IllegalArgumentException e) {
            return error("badArgument", e.getMessage(), request);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            return error("badArgument", "Permintaan tidak dapat diproses.", request);
        }
    }

    private static Result identify(HttpServletRequest request) {
        String body = "<Identify><repositoryName>" + xml(repositoryName()) + "</repositoryName>"
                + "<baseURL>" + xml(baseUrl(request)) + "</baseURL><protocolVersion>2.0</protocolVersion>"
                + "<adminEmail>" + xml(config("library.oai.admin_email", "admin@localhost")) + "</adminEmail>"
                + "<earliestDatestamp>1970-01-01T00:00:00Z</earliestDatestamp>"
                + "<deletedRecord>no</deletedRecord><granularity>YYYY-MM-DDThh:mm:ssZ</granularity></Identify>";
        return ok(envelope(request, body));
    }

    private static Result formats(HttpServletRequest request) {
        String body = "<ListMetadataFormats><metadataFormat><metadataPrefix>oai_dc</metadataPrefix>"
                + "<schema>http://www.openarchives.org/OAI/2.0/oai_dc.xsd</schema>"
                + "<metadataNamespace>http://www.openarchives.org/OAI/2.0/oai_dc/</metadataNamespace>"
                + "</metadataFormat></ListMetadataFormats>";
        return ok(envelope(request, body));
    }

    @SuppressWarnings("unchecked")
    private static Result getRecord(HttpServletRequest request) {
        requirePrefix(request.getParameter("metadataPrefix"));
        Long id = identifier(request.getParameter("identifier"));
        if (id == null) return error("idDoesNotExist", "Identifier tidak ditemukan.", request);
        Session session = null;
        try {
            session = HibernateUtil.openSession();
            Query query = session.createSQLQuery(selectSql() + whereScope() + " and i.id=:id");
            query.setLong("id", id.longValue());
            bindLibrary(query);
            List<Object[]> rows = query.list();
            if (rows.isEmpty()) return error("idDoesNotExist", "Identifier tidak ditemukan.", request);
            return ok(envelope(request, "<GetRecord>" + record(rows.get(0)) + "</GetRecord>"));
        } finally { HibernateUtil.closeSessionQuietly(session); }
    }

    @SuppressWarnings("unchecked")
    private static Result list(HttpServletRequest request, boolean includeMetadata) throws ParseException {
        int offset = tokenOffset(request.getParameter("resumptionToken"));
        if (request.getParameter("resumptionToken") == null) requirePrefix(request.getParameter("metadataPrefix"));
        Date from = date(request.getParameter("from"));
        Date until = date(request.getParameter("until"));
        if (from != null && until != null && from.after(until)) throw new IllegalArgumentException("Rentang tanggal tidak valid.");

        Session session = null;
        try {
            session = HibernateUtil.openSession();
            String filters = whereScope();
            if (from != null) filters += " and i.tanggal_dirubah>=:from";
            if (until != null) filters += " and i.tanggal_dirubah<=:until";
            Query count = session.createSQLQuery("select count(i.id) from library.item i " + filters);
            bind(count, from, until); bindLibrary(count);
            long total = number(count.uniqueResult());
            if (total == 0) return error("noRecordsMatch", "Tidak ada record yang sesuai.", request);

            Query query = session.createSQLQuery(selectSql() + filters + " order by i.id");
            bind(query, from, until); bindLibrary(query);
            query.setFirstResult(offset).setMaxResults(PAGE_SIZE);
            List<Object[]> rows = query.list();
            String tag = includeMetadata ? "ListRecords" : "ListIdentifiers";
            StringBuilder body = new StringBuilder("<").append(tag).append(">");
            for (Object[] row : rows) body.append(includeMetadata ? record(row) : header(row));
            int next = offset + rows.size();
            if (next < total) body.append("<resumptionToken completeListSize=\"").append(total)
                    .append("\" cursor=\"").append(offset).append("\">offset:").append(next).append("</resumptionToken>");
            else if (offset > 0) body.append("<resumptionToken completeListSize=\"").append(total)
                    .append("\" cursor=\"").append(offset).append("\"></resumptionToken>");
            body.append("</").append(tag).append(">");
            return ok(envelope(request, body.toString()));
        } finally { HibernateUtil.closeSessionQuietly(session); }
    }

    private static String selectSql() {
        return "select i.id,coalesce(i.kode,''),coalesce(i.nama,''),coalesce(i.pengarangs,''),"
                + "coalesce(p.nama,''),i.tahun,coalesce(i.isbn,''),coalesce(i.issn,''),"
                + "coalesce(i.bahasa,''),coalesce(i.abstrak,''),i.tanggal_dirubah "
                + "from library.item i left join library.penerbit p on p.id=i.penerbit ";
    }

    private static String whereScope() {
        Perpustakaan library = Common.getCurrentPerpustakaan();
        String sql = " where coalesce(i.aktif,true)=true";
        if (library != null && library.getId() != null) {
            sql += " and (not exists(select 1 from library.item_punya_barcode x where x.item=i.id)"
                    + " or exists(select 1 from library.item_punya_barcode x where x.item=i.id and x.perpustakaan=:library))";
        }
        return sql;
    }

    private static void bindLibrary(Query query) {
        Perpustakaan library = Common.getCurrentPerpustakaan();
        if (library != null && library.getId() != null) query.setLong("library", library.getId().longValue());
    }

    private static void bind(Query query, Date from, Date until) {
        if (from != null) query.setTimestamp("from", from);
        if (until != null) query.setTimestamp("until", until);
    }

    private static String record(Object[] row) {
        return "<record>" + header(row) + "<metadata><oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\""
                + " xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">"
                + dc("identifier", identifier(number(row[0]))) + dc("identifier", string(row[1]))
                + dc("title", string(row[2])) + dc("creator", string(row[3])) + dc("publisher", string(row[4]))
                + dc("date", string(row[5])) + dc("identifier", string(row[6])) + dc("identifier", string(row[7]))
                + dc("language", string(row[8])) + dc("description", string(row[9]))
                + "</oai_dc:dc></metadata></record>";
    }

    private static String header(Object[] row) {
        return "<header><identifier>" + xml(identifier(number(row[0]))) + "</identifier><datestamp>"
                + xml(timestamp(row[10])) + "</datestamp></header>";
    }

    private static String dc(String name, String value) {
        return value == null || value.trim().length() == 0 ? "" : "<dc:" + name + ">" + xml(value) + "</dc:" + name + ">";
    }

    private static Result error(String code, String message, HttpServletRequest request) {
        return new Result(200, envelope(request, "<error code=\"" + xml(code) + "\">" + xml(message) + "</error>"));
    }

    private static Result ok(String xml) { return new Result(200, xml); }
    private static String envelope(HttpServletRequest request, String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">"
                + "<responseDate>" + timestamp(new Date()) + "</responseDate><request>" + xml(baseUrl(request)) + "</request>" + body + "</OAI-PMH>";
    }

    private static void requirePrefix(String value) {
        if (!"oai_dc".equals(value)) throw new IllegalArgumentException("metadataPrefix harus oai_dc.");
    }
    private static int tokenOffset(String token) {
        if (token == null || token.trim().length() == 0) return 0;
        if (!token.matches("offset:[0-9]{1,9}")) throw new IllegalArgumentException("resumptionToken tidak valid.");
        return Integer.parseInt(token.substring(7));
    }
    private static Date date(String value) throws ParseException {
        if (value == null || value.trim().length() == 0) return null;
        String pattern = value.length() == 10 ? "yyyy-MM-dd" : "yyyy-MM-dd'T'HH:mm:ss'Z'";
        SimpleDateFormat format = new SimpleDateFormat(pattern); format.setLenient(false); format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.parse(value);
    }
    private static String timestamp(Object value) {
        if (!(value instanceof Date)) return "1970-01-01T00:00:00Z";
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format((Date) value);
    }
    private static Long identifier(String value) {
        if (value == null) return null;
        String prefix = "oai:ais.library:item:";
        if (!value.startsWith(prefix)) return null;
        try { long id=Long.parseLong(value.substring(prefix.length())); return id>0?Long.valueOf(id):null; } catch(Exception e){ return null; }
    }
    private static String identifier(long id) { return "oai:ais.library:item:" + id; }
    private static String baseUrl(HttpServletRequest r) { return r.getRequestURL().toString(); }
    private static String repositoryName() { return config("library.oai.repository_name", "AIS Library Repository"); }
    private static String config(String key, String fallback) { try { String v=Common.getKonfigurasi(key,fallback).getNilai(); return v==null||v.trim().length()==0?fallback:v.trim(); } catch(Exception e){ return fallback; } }
    private static String text(String value,int max){if(value==null||value.trim().length()==0)return null;value=value.trim();return value.length()>max?value.substring(0,max):value;}
    private static String string(Object value){return value==null?"":String.valueOf(value);}
    private static long number(Object value){return value instanceof Number?((Number)value).longValue():0L;}
    private static String xml(String value){if(value==null)return "";return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}

    /**
     * Pembawa data/helper lokal milik {@link LibraryOaiPmhService} untuk result. Tipe ini mengelompokkan nilai
     * antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link LibraryOaiPmhService}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int status}, {@code String body};
     * operasi lokal: {@code getStatus()}, {@code getBody}(). Aturan bisnis bersama tetap berada pada kelas induk
     * atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see LibraryOaiPmhService
     */
    public static final class Result {
        private final int status; private final String body;
        private Result(int status,String body){this.status=status;this.body=body;}
        public int getStatus(){return status;} public String getBody(){return body;}
    }
}
