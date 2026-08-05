package ais.action.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.repository.RepoCollection;
import ais.database.model.repository.RepoItem;
import ais.database.model.repository.RepoItemMetadata;

/**
 * OAI-PMH 2.0 servlet — exposes repository metadata to external harvesters.
 * Endpoint: /oai  (public, no login required)
 *
 * Verbs supported: Identify, ListMetadataFormats, ListSets,
 *                  GetRecord, ListIdentifiers, ListRecords
 * Metadata format: oai_dc (Dublin Core)
 */
public class Oai extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** Records returned per page for ListIdentifiers / ListRecords. */
    private static final int PAGE_SIZE = 100;

    private static final String OAI_NS =
        "http://www.openarchives.org/OAI/2.0/";
    private static final String OAI_SCHEMA =
        "http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd";
    private static final String DC_NS =
        "http://purl.org/dc/elements/1.1/";
    private static final String OAI_DC_NS =
        "http://www.openarchives.org/OAI/2.0/oai_dc/";
    private static final String OAI_DC_SCHEMA =
        "http://www.openarchives.org/OAI/2.0/oai_dc.xsd";

    private static final String FMT_FULL = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final String FMT_DAY  = "yyyy-MM-dd";

    private static SimpleDateFormat fullFmt() {
        SimpleDateFormat sdf = new SimpleDateFormat(FMT_FULL);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf;
    }
    private static SimpleDateFormat dayFmt() {
        SimpleDateFormat sdf = new SimpleDateFormat(FMT_DAY);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf;
    }

    public Oai() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        process(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        process(request, response);
    }

    // ── Main dispatcher ───────────────────────────────────────────────────────

    private void process(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        /* setCharacterEncoding tidak ada di Servlet API lama (build server);
         * charset sudah ditetapkan lewat setContentType di bawah. */
        response.setContentType("text/xml; charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Access-Control-Allow-Origin", "*");

        PrintWriter out = response.getWriter();

        String baseUrl  = buildBaseUrl(request);
        String now      = fullFmt().format(new Date());
        String verb     = param(request, "verb");

        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<OAI-PMH xmlns=\"" + OAI_NS + "\"");
        out.println("  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        out.println("  xsi:schemaLocation=\"" + OAI_NS + " " + OAI_SCHEMA + "\">");
        out.println("  <responseDate>" + now + "</responseDate>");

        if (verb == null || verb.trim().isEmpty()) {
            writeRequestTag(out, baseUrl, null, request);
            writeError(out, "badVerb", "Missing verb argument.");
        } else {
            /* switch-on-String adalah fitur Java 7; build server memakai
             * -source 1.6 sehingga dipakai rantai if/else equals. */
            if ("Identify".equals(verb)) {
                writeRequestTag(out, baseUrl, verb, request);
                handleIdentify(out, request, baseUrl);
            } else if ("ListMetadataFormats".equals(verb)) {
                writeRequestTag(out, baseUrl, verb, request);
                handleListMetadataFormats(out, request);
            } else if ("ListSets".equals(verb)) {
                writeRequestTag(out, baseUrl, verb, request);
                handleListSets(out);
            } else if ("GetRecord".equals(verb)) {
                writeRequestTag(out, baseUrl, verb, request);
                handleGetRecord(out, request);
            } else if ("ListIdentifiers".equals(verb)) {
                writeRequestTag(out, baseUrl, verb, request);
                handleListIdentifiers(out, request);
            } else if ("ListRecords".equals(verb)) {
                writeRequestTag(out, baseUrl, verb, request);
                handleListRecords(out, request);
            } else {
                writeRequestTag(out, baseUrl, null, request);
                writeError(out, "badVerb", "Illegal verb: " + escXml(verb));
            }
        }

        out.println("</OAI-PMH>");
        out.flush();
        out.close();
    }

    // ── Verb: Identify ────────────────────────────────────────────────────────

    private void handleIdentify(PrintWriter out, HttpServletRequest request, String baseUrl) {
        String repoName   = Common.getKonfigurasi("oai_repository_name",
                            Common.getKonfigurasi("nama_institusi", "Repository").getNilai())
                            .getNilai();
        String adminEmail = Common.getKonfigurasi("oai_admin_email",
                            Common.getKonfigurasi("email_institusi", "admin@repository.ac.id").getNilai())
                            .getNilai();
        String earliest   = queryEarliestDatestamp();
        String desc       = Common.getKonfigurasi("oai_repository_description",
                            repoName + " — Open Repository").getNilai();

        out.println("  <Identify>");
        out.println("    <repositoryName>" + escXml(repoName) + "</repositoryName>");
        out.println("    <baseURL>" + escXml(baseUrl) + "</baseURL>");
        out.println("    <protocolVersion>2.0</protocolVersion>");
        out.println("    <adminEmail>" + escXml(adminEmail) + "</adminEmail>");
        out.println("    <earliestDatestamp>" + earliest + "</earliestDatestamp>");
        out.println("    <deletedRecord>transient</deletedRecord>");
        out.println("    <granularity>YYYY-MM-DDThh:mm:ssZ</granularity>");
        out.println("    <description>");
        out.println("      <oai-identifier");
        out.println("        xmlns=\"http://www.openarchives.org/OAI/2.0/oai-identifier\"");
        out.println("        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        out.println("        xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai-identifier");
        out.println("          http://www.openarchives.org/OAI/2.0/oai-identifier.xsd\">");
        out.println("        <scheme>oai</scheme>");
        out.println("        <repositoryIdentifier>" + escXml(buildRepositoryIdentifier(request)) + "</repositoryIdentifier>");
        out.println("        <delimiter>:</delimiter>");
        out.println("        <sampleIdentifier>oai:" + escXml(buildRepositoryIdentifier(request)) + ":repo/1</sampleIdentifier>");
        out.println("      </oai-identifier>");
        out.println("    </description>");
        out.println("    <description>");
        out.println("      <friends xmlns=\"http://www.openarchives.org/OAI/2.0/friends/\"");
        out.println("               xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        out.println("               xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/friends/");
        out.println("                 http://www.openarchives.org/OAI/2.0/friends.xsd\">");
        out.println("        <shortDescription>" + escXml(desc) + "</shortDescription>");
        out.println("      </friends>");
        out.println("    </description>");
        out.println("  </Identify>");
    }

    // ── Verb: ListMetadataFormats ─────────────────────────────────────────────

    private void handleListMetadataFormats(PrintWriter out, HttpServletRequest request) {
        String identifier = param(request, "identifier");
        if (identifier != null && !identifier.trim().isEmpty()) {
            RepoItem item = findByIdentifier(identifier.trim());
            if (item == null) {
                writeError(out, "idDoesNotExist",
                    "No matching record: " + escXml(identifier));
                return;
            }
        }
        out.println("  <ListMetadataFormats>");
        out.println("    <metadataFormat>");
        out.println("      <metadataPrefix>oai_dc</metadataPrefix>");
        out.println("      <schema>" + OAI_DC_SCHEMA + "</schema>");
        out.println("      <metadataNamespace>" + OAI_DC_NS + "</metadataNamespace>");
        out.println("    </metadataFormat>");
        out.println("  </ListMetadataFormats>");
    }

    // ── Verb: ListSets ────────────────────────────────────────────────────────

    private void handleListSets(PrintWriter out) {
        Session session = null;
        try {
            session = openSession();
            @SuppressWarnings("unchecked")
            List<RepoCollection> cols = session.createCriteria(RepoCollection.class)
                .add(Restrictions.eq("aktif", Boolean.TRUE))
                .addOrder(Order.asc("sortOrder"))
                .addOrder(Order.asc("nama"))
                .list();

            if (cols == null || cols.isEmpty()) {
                writeError(out, "noSetHierarchy", "This repository has no sets.");
                return;
            }

            out.println("  <ListSets>");
            for (RepoCollection col : cols) {
                out.println("    <set>");
                out.println("      <setSpec>col_" + col.getId() + "</setSpec>");
                out.println("      <setName>" + escXml(col.getNama()) + "</setName>");
                if (col.getDeskripsi() != null && !col.getDeskripsi().trim().isEmpty()) {
                    out.println("      <setDescription>");
                    out.println("        <oai_dc:dc xmlns:oai_dc=\"" + OAI_DC_NS + "\"");
                    out.println("                   xmlns:dc=\"" + DC_NS + "\"");
                    out.println("                   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
                    out.println("                   xsi:schemaLocation=\"" + OAI_DC_NS + " " + OAI_DC_SCHEMA + "\">");
                    out.println("          <dc:description xmlns:dc=\"" + DC_NS + "\">"
                        + escXml(col.getDeskripsi()) + "</dc:description>");
                    out.println("        </oai_dc:dc>");
                    out.println("      </setDescription>");
                }
                out.println("    </set>");
            }
            out.println("  </ListSets>");
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Oai.java:250");
            writeError(out, "noSetHierarchy", "Error retrieving sets: " + escXml(e.getMessage()));
        } finally {
            closeSession(session);
        }
    }

    // ── Verb: GetRecord ───────────────────────────────────────────────────────

    private void handleGetRecord(PrintWriter out, HttpServletRequest request) {
        String identifier      = param(request, "identifier");
        String metadataPrefix  = param(request, "metadataPrefix");

        if (identifier == null || identifier.trim().isEmpty()) {
            writeError(out, "badArgument", "Missing argument: identifier.");
            return;
        }
        if (metadataPrefix == null || metadataPrefix.trim().isEmpty()) {
            writeError(out, "badArgument", "Missing argument: metadataPrefix.");
            return;
        }
        if (!"oai_dc".equals(metadataPrefix)) {
            writeError(out, "cannotDisseminateFormat",
                "Metadata format not supported: " + escXml(metadataPrefix));
            return;
        }

        RepoItem item = findByIdentifier(identifier.trim());
        if (item == null) {
            writeError(out, "idDoesNotExist",
                "No record with identifier: " + escXml(identifier));
            return;
        }

        Session session = null;
        try {
            session = openSession();
            List<RepoItemMetadata> metas = loadMetadata(session, item.getId());
            String setSpec = "col_" + item.getCollectionId();

            out.println("  <GetRecord>");
            writeRecord(out, session, item, metas, setSpec);
            out.println("  </GetRecord>");
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Oai.java:294");
            writeError(out, "badArgument", "Error retrieving record: " + escXml(e.getMessage()));
        } finally {
            closeSession(session);
        }
    }

    // ── Verb: ListIdentifiers ─────────────────────────────────────────────────

    private void handleListIdentifiers(PrintWriter out, HttpServletRequest request) {
        ListQuery q = parseListQuery(request);
        if (q.error != null) {
            writeError(out, q.errorCode, q.error);
            return;
        }

        Session session = null;
        try {
            session = openSession();
            long total = countItems(session, q);
            if (total == 0) {
                writeError(out, "noRecordsMatch", "No records match the query.");
                return;
            }

            List<RepoItem> items = listItems(session, q, q.offset, PAGE_SIZE);

            out.println("  <ListIdentifiers>");
            for (RepoItem item : items) {
                String setSpec = "col_" + item.getCollectionId();
                String datestamp = itemDatestamp(item);
                out.println("    <header" + (item.getIsWithdrawn() ? " status=\"deleted\"" : "") + ">");
                out.println("      <identifier>" + escXml(item.getOaiIdentifier()) + "</identifier>");
                out.println("      <datestamp>" + datestamp + "</datestamp>");
                out.println("      <setSpec>" + setSpec + "</setSpec>");
                out.println("    </header>");
            }
            writeResumptionToken(out, q, total, items.size());
            out.println("  </ListIdentifiers>");
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Oai.java:334");
            writeError(out, "noRecordsMatch", "Error: " + escXml(e.getMessage()));
        } finally {
            closeSession(session);
        }
    }

    // ── Verb: ListRecords ─────────────────────────────────────────────────────

    private void handleListRecords(PrintWriter out, HttpServletRequest request) {
        ListQuery q = parseListQuery(request);
        if (q.error != null) {
            writeError(out, q.errorCode, q.error);
            return;
        }

        Session session = null;
        try {
            session = openSession();
            long total = countItems(session, q);
            if (total == 0) {
                writeError(out, "noRecordsMatch", "No records match the query.");
                return;
            }

            List<RepoItem> items = listItems(session, q, q.offset, PAGE_SIZE);

            out.println("  <ListRecords>");
            for (RepoItem item : items) {
                String setSpec = "col_" + item.getCollectionId();
                List<RepoItemMetadata> metas = loadMetadata(session, item.getId());
                writeRecord(out, session, item, metas, setSpec);
            }
            writeResumptionToken(out, q, total, items.size());
            out.println("  </ListRecords>");
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Oai.java:370");
            writeError(out, "noRecordsMatch", "Error: " + escXml(e.getMessage()));
        } finally {
            closeSession(session);
        }
    }

    // ── Record XML writer ─────────────────────────────────────────────────────

    private void writeRecord(PrintWriter out, Session session, RepoItem item,
                              List<RepoItemMetadata> metas, String setSpec) {
        String datestamp = itemDatestamp(item);
        boolean deleted  = item.getIsWithdrawn();

        out.println("    <record>");
        out.println("      <header" + (deleted ? " status=\"deleted\"" : "") + ">");
        out.println("        <identifier>" + escXml(item.getOaiIdentifier()) + "</identifier>");
        out.println("        <datestamp>" + datestamp + "</datestamp>");
        out.println("        <setSpec>" + setSpec + "</setSpec>");
        out.println("      </header>");

        if (!deleted) {
            out.println("      <metadata>");
            out.println("        <oai_dc:dc");
            out.println("          xmlns:oai_dc=\"" + OAI_DC_NS + "\"");
            out.println("          xmlns:dc=\"" + DC_NS + "\"");
            out.println("          xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
            out.println("          xsi:schemaLocation=\"" + OAI_DC_NS + " " + OAI_DC_SCHEMA + "\">");

            Map<String, List<String>> dc = buildDublinCore(item, metas);
            for (Map.Entry<String, List<String>> entry : dc.entrySet()) {
                String element = entry.getKey();
                for (String val : entry.getValue()) {
                    if (val != null && !val.trim().isEmpty()) {
                        out.println("          <dc:" + element + ">" + escXml(val.trim()) + "</dc:" + element + ">");
                    }
                }
            }

            out.println("        </oai_dc:dc>");
            out.println("      </metadata>");
        }
        out.println("    </record>");
    }

    // ── Dublin Core mapping ───────────────────────────────────────────────────

    /**
     * Builds Dublin Core elements from entity fields, then enriches/overrides
     * from the EAV metadata table (EAV takes priority over entity fields).
     */
    private Map<String, List<String>> buildDublinCore(RepoItem item,
                                                       List<RepoItemMetadata> metas) {
        Map<String, List<String>> dc = new LinkedHashMap<String, List<String>>();
        dc.put("title",       listOf(item.getTitle()));
        dc.put("creator",     splitLines(item.getAuthors()));
        dc.put("subject",     splitLines(item.getSubjects()));
        dc.put("description", listOf(item.getAbstractText()));
        dc.put("publisher",   listOf(item.getPublisher()));
        dc.put("contributor", new ArrayList<String>());
        dc.put("date",        listOf(formatDate(item.getIssuedAt())));
        dc.put("type",        listOf(item.getDocumentType()));
        dc.put("format",      new ArrayList<String>());
        dc.put("identifier",  buildIdentifiers(item));
        dc.put("source",      new ArrayList<String>());
        dc.put("language",    listOf(item.getLanguage()));
        dc.put("relation",    new ArrayList<String>());
        dc.put("coverage",    new ArrayList<String>());
        dc.put("rights",      listOf(item.getAccessPolicy()));

        // EAV overrides — map dc.field to Dublin Core element
        Map<String, String> fieldMap = new LinkedHashMap<String, String>();
        fieldMap.put("dc.title",                 "title");
        fieldMap.put("dc.contributor.author",    "creator");
        fieldMap.put("dc.contributor",           "contributor");
        fieldMap.put("dc.subject",               "subject");
        fieldMap.put("dc.description.abstract",  "description");
        fieldMap.put("dc.description",           "description");
        fieldMap.put("dc.publisher",             "publisher");
        fieldMap.put("dc.date.issued",           "date");
        fieldMap.put("dc.date.available",        "date");
        fieldMap.put("dc.date.created",          "date");
        fieldMap.put("dc.type",                  "type");
        fieldMap.put("dc.format",                "format");
        fieldMap.put("dc.identifier",            "identifier");
        fieldMap.put("dc.identifier.uri",        "identifier");
        fieldMap.put("dc.identifier.isbn",       "identifier");
        fieldMap.put("dc.identifier.issn",       "identifier");
        fieldMap.put("dc.source",                "source");
        fieldMap.put("dc.language",              "language");
        fieldMap.put("dc.language.iso",          "language");
        fieldMap.put("dc.relation",              "relation");
        fieldMap.put("dc.coverage",              "coverage");
        fieldMap.put("dc.rights",                "rights");
        fieldMap.put("dc.rights.license",        "rights");

        if (metas != null && !metas.isEmpty()) {
            // Reset values when EAV has data for the primary fields
            boolean hasDcTitle     = false, hasDcCreator = false;
            boolean hasDcSubject   = false, hasDcDesc    = false;
            boolean hasDcPublisher = false, hasDcDate    = false;
            boolean hasDcType      = false, hasDcLang    = false;
            boolean hasDcRights    = false;

            for (RepoItemMetadata m : metas) {
                String field = m.getMetadataField();
                if (field == null) continue;
                field = field.trim();
                String target = fieldMap.get(field);
                if (target == null) continue;
                String val = m.getMetadataValue();
                if (val == null || val.trim().isEmpty()) continue;

                // Clear entity defaults on first EAV hit for each element
                if (!hasDcTitle     && "title".equals(target))       { dc.get("title").clear();  hasDcTitle = true; }
                if (!hasDcCreator   && "creator".equals(target))     { dc.get("creator").clear(); hasDcCreator = true; }
                if (!hasDcSubject   && "subject".equals(target))     { dc.get("subject").clear(); hasDcSubject = true; }
                if (!hasDcDesc      && "description".equals(target)) { dc.get("description").clear(); hasDcDesc = true; }
                if (!hasDcPublisher && "publisher".equals(target))   { dc.get("publisher").clear(); hasDcPublisher = true; }
                if (!hasDcDate      && "date".equals(target))        { dc.get("date").clear();   hasDcDate = true; }
                if (!hasDcType      && "type".equals(target))        { dc.get("type").clear();   hasDcType = true; }
                if (!hasDcLang      && "language".equals(target))    { dc.get("language").clear(); hasDcLang = true; }
                if (!hasDcRights    && "rights".equals(target))      { dc.get("rights").clear(); hasDcRights = true; }

                dc.get(target).add(val.trim());
            }
        }

        return dc;
    }

    private List<String> buildIdentifiers(RepoItem item) {
        List<String> ids = new ArrayList<String>();
        if (item.getOaiIdentifier() != null && !item.getOaiIdentifier().isEmpty())
            ids.add(item.getOaiIdentifier());
        if (item.getDspaceHandle() != null && !item.getDspaceHandle().isEmpty())
            ids.add("https://hdl.handle.net/" + item.getDspaceHandle());
        return ids;
    }

    // ── Query helpers ─────────────────────────────────────────────────────────

    private Criteria buildItemCriteria(Session session, ListQuery q) {
        Criteria c = session.createCriteria(RepoItem.class);
        // Only expose active items (or withdrawn as deleted)
        c.add(Restrictions.eq("aktif", Boolean.TRUE));

        if (q.setSpec != null) {
            // setSpec format: col_<id>
            try {
                Long colId = Long.parseLong(q.setSpec.replaceFirst("^col_", ""));
                c.add(Restrictions.eq("collectionId", colId));
            } catch (NumberFormatException ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Oai.java:522");}
        }
        if (q.from != null)
            c.add(Restrictions.ge("submittedAt", q.from));
        if (q.until != null)
            c.add(Restrictions.le("submittedAt", q.until));

        return c;
    }

    @SuppressWarnings("unchecked")
    private List<RepoItem> listItems(Session session, ListQuery q, int offset, int limit) {
        Criteria c = buildItemCriteria(session, q);
        c.addOrder(Order.asc("id"));
        c.setFirstResult(offset);
        c.setMaxResults(limit);
        return c.list();
    }

    private long countItems(Session session, ListQuery q) {
        Criteria c = buildItemCriteria(session, q);
        c.setProjection(Projections.rowCount());
        Object result = c.uniqueResult();
        return result == null ? 0L : ((Number) result).longValue();
    }

    @SuppressWarnings("unchecked")
    private List<RepoItemMetadata> loadMetadata(Session session, Long itemId) {
        return session.createCriteria(RepoItemMetadata.class)
            .add(Restrictions.eq("itemId", itemId))
            .add(Restrictions.eq("aktif", Boolean.TRUE))
            .addOrder(Order.asc("metadataField"))
            .addOrder(Order.asc("place"))
            .list();
    }

    private RepoItem findByIdentifier(String identifier) {
        Session session = null;
        try {
            session = openSession();
            return (RepoItem) session.createCriteria(RepoItem.class)
                .add(Restrictions.eq("oaiIdentifier", identifier))
                .setMaxResults(1)
                .uniqueResult();
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Oai.java:567");
            return null;
        } finally {
            closeSession(session);
        }
    }

    private String queryEarliestDatestamp() {
        Session session = null;
        try {
            session = openSession();
            Object result = session.createCriteria(RepoItem.class)
                .add(Restrictions.eq("aktif", Boolean.TRUE))
                .add(Restrictions.isNotNull("submittedAt"))
                .setProjection(Projections.min("submittedAt"))
                .uniqueResult();
            if (result instanceof Date)
                return fullFmt().format((Date) result);
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Oai.java:586");
        } finally {
            closeSession(session);
        }
        return "2000-01-01T00:00:00Z";
    }

    // ── Resumption token ──────────────────────────────────────────────────────

    private void writeResumptionToken(PrintWriter out, ListQuery q,
                                       long total, int fetched) {
        int nextOffset = q.offset + fetched;
        boolean hasMore = nextOffset < total;

        if (hasMore) {
            String token = encodeToken(q.from, q.until, q.setSpec, nextOffset, total);
            out.println("    <resumptionToken completeListSize=\"" + total + "\""
                + " cursor=\"" + q.offset + "\">" + token + "</resumptionToken>");
        } else if (q.offset > 0) {
            // Last page: emit empty token to signal completion
            out.println("    <resumptionToken completeListSize=\"" + total + "\""
                + " cursor=\"" + q.offset + "\"/>");
        }
    }

    /**
     * Token format (Base64 encoded): from|until|set|offset|total
     * Missing values are encoded as empty string.
     */
    private String encodeToken(Date from, Date until, String set,
                                 int offset, long total) {
        SimpleDateFormat _f = fullFmt();
        String f = from  != null ? _f.format(from)  : "";
        String u = until != null ? _f.format(until) : "";
        String s = set   != null ? set : "";
        String raw = f + "|" + u + "|" + s + "|" + offset + "|" + total;
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private ListQuery decodeToken(String token) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(token);
            String raw = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|", -1);
            if (parts.length < 5) return null;

            ListQuery q = new ListQuery();
            q.from   = parts[0].isEmpty() ? null : parseDateFlexible(parts[0]);
            q.until  = parts[1].isEmpty() ? null : parseDateFlexible(parts[1]);
            q.setSpec = parts[2].isEmpty() ? null : parts[2];
            q.offset = Integer.parseInt(parts[3]);
            q.metadataPrefix = "oai_dc";
            return q;
        } catch (Exception e) {
            return null;
        }
    }

    // ── ListQuery parser ──────────────────────────────────────────────────────

    private ListQuery parseListQuery(HttpServletRequest request) {
        ListQuery q = new ListQuery();
        String token = param(request, "resumptionToken");

        if (token != null && !token.trim().isEmpty()) {
            // When resumptionToken is provided, no other args allowed (except verb)
            ListQuery decoded = decodeToken(token.trim());
            if (decoded == null) {
                q.errorCode = "badResumptionToken";
                q.error = "Invalid or expired resumption token.";
                return q;
            }
            return decoded;
        }

        q.metadataPrefix = param(request, "metadataPrefix");
        if (q.metadataPrefix == null || q.metadataPrefix.trim().isEmpty()) {
            q.errorCode = "badArgument";
            q.error = "Missing required argument: metadataPrefix.";
            return q;
        }
        if (!"oai_dc".equals(q.metadataPrefix.trim())) {
            q.errorCode = "cannotDisseminateFormat";
            q.error = "Metadata format not supported: " + escXml(q.metadataPrefix);
            return q;
        }

        String fromStr  = param(request, "from");
        String untilStr = param(request, "until");
        String setSpec  = param(request, "set");

        try {
            q.from = (fromStr  != null && !fromStr.trim().isEmpty())
                ? parseDateFlexible(fromStr.trim()) : null;
        } catch (ParseException e) {
            q.errorCode = "badArgument";
            q.error = "Invalid date format for 'from': " + escXml(fromStr);
            return q;
        }
        try {
            q.until = (untilStr != null && !untilStr.trim().isEmpty())
                ? parseDateUntilEndOfDay(untilStr.trim()) : null;
        } catch (ParseException e) {
            q.errorCode = "badArgument";
            q.error = "Invalid date format for 'until': " + escXml(untilStr);
            return q;
        }

        q.setSpec = (setSpec != null && !setSpec.trim().isEmpty()) ? setSpec.trim() : null;
        q.offset  = 0;
        return q;
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private static class ListQuery {
        String metadataPrefix;
        Date   from;
        Date   until;
        String setSpec;
        int    offset;
        String error;
        String errorCode = "badArgument";
    }

    private String buildBaseUrl(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.getScheme()).append("://").append(request.getServerName());
        int port = request.getServerPort();
        if (!((request.getScheme().equals("http")  && port == 80) ||
              (request.getScheme().equals("https") && port == 443))) {
            sb.append(":").append(port);
        }
        sb.append(request.getContextPath()).append("/oai");
        return sb.toString();
    }

    private String buildRepositoryIdentifier(HttpServletRequest request) {
        return request.getServerName();
    }

    private String itemDatestamp(RepoItem item) {
        Date d = item.getLastSyncAt() != null ? item.getLastSyncAt()
               : item.getSubmittedAt() != null ? item.getSubmittedAt()
               : item.getTanggal_dirubah();
        SimpleDateFormat f = fullFmt();
        return d != null ? f.format(d) : f.format(new Date());
    }

    private String formatDate(Date d) {
        if (d == null) return "";
        return dayFmt().format(d);
    }

    private Date parseDateFlexible(String s) throws ParseException {
        if (s.length() == 10)
            return dayFmt().parse(s);
        return fullFmt().parse(s);
    }

    private Date parseDateUntilEndOfDay(String s) throws ParseException {
        if (s.length() == 10)
            return fullFmt().parse(s + "T23:59:59Z");
        return parseDateFlexible(s);
    }

    private List<String> listOf(String val) {
        List<String> list = new ArrayList<String>();
        if (val != null && !val.trim().isEmpty())
            list.add(val.trim());
        return list;
    }

    private List<String> splitLines(String val) {
        List<String> list = new ArrayList<String>();
        if (val == null || val.trim().isEmpty()) return list;
        for (String part : val.split("[\\n;,]+")) {
            String t = part.trim();
            if (!t.isEmpty()) list.add(t);
        }
        return list;
    }

    private String param(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        return v == null ? null : v.trim().isEmpty() ? null : v.trim();
    }

    private String escXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private void writeRequestTag(PrintWriter out, String baseUrl,
                                  String verb, HttpServletRequest request) {
        StringBuilder attrs = new StringBuilder();
        if (verb != null) attrs.append(" verb=\"").append(escXml(verb)).append("\"");

        String[] attrNames = {"identifier", "metadataPrefix", "from", "until",
                               "set", "resumptionToken"};
        for (String a : attrNames) {
            String v = param(request, a);
            if (v != null)
                attrs.append(" ").append(a).append("=\"").append(escXml(v)).append("\"");
        }
        out.println("  <request" + attrs + ">" + escXml(baseUrl) + "</request>");
    }

    private void writeError(PrintWriter out, String code, String message) {
        out.println("  <error code=\"" + escXml(code) + "\">" + escXml(message) + "</error>");
    }

    // ── Hibernate helpers ─────────────────────────────────────────────────────

    private Session openSession() {
        return StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
    }

    private void closeSession(Session session) {
        if (session != null && session.isOpen()) {
            try { session.clear(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Oai.java:811");}
            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Oai.java:812");}
        }
    }
}
