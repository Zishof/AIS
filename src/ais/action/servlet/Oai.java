package ais.action.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

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
import ais.action.master.jurnal.JurnalMetadataFormatService;
import ais.action.master.jurnal.JurnalRateLimiter;
import ais.action.master.repository.RepositoryTenantScope;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.repository.RepoCollection;
import ais.database.model.repository.RepoItem;
import ais.database.model.repository.RepoItemMetadata;

/**
 * Servlet OAI-PMH 2.0 (Open Archives Initiative Protocol for Metadata Harvesting) mandiri --
 * memaparkan metadata repositori institusi ({@link RepoItem}/{@link RepoCollection}/{@link
 * RepoItemMetadata}) ke pemanen (harvester) eksternal lewat endpoint {@code /oai}, publik tanpa
 * login. Verb yang didukung: {@code Identify}, {@code ListMetadataFormats}, {@code ListSets},
 * {@code GetRecord}, {@code ListIdentifiers}, {@code ListRecords}; satu-satunya format metadata
 * yang diserialisasi adalah {@code oai_dc} (Dublin Core, lewat {@link
 * JurnalMetadataFormatService}).
 *
 * <p><b>Relasi dengan {@code Repository.oai()}:</b> file ini BUKAN pemanggil maupun turunan dari
 * method privat {@code oai(HttpServletRequest,HttpServletResponse)} pada {@link Repository}
 * (dipetakan ke {@code /repository?action=oai}). Keduanya adalah DUA IMPLEMENTASI OAI-PMH 2.0
 * TERPISAH yang mengekspos model data repositori yang SAMA ({@link RepoItem}/{@link
 * RepoCollection}, via {@link ais.action.master.repository.RepositoryPublicService} pada sisi
 * {@code Repository}), dipetakan ke dua URL berbeda ({@code /oai} di sini vs {@code
 * /repository?action=oai}), dengan mekanisme resumption-token, rate limiting, dan cakupan tenant
 * yang dikodekan ulang secara independen di masing-masing sisi. Ini adalah duplikasi/drift
 * arsitektur yang perlu diwaspadai saat mengubah salah satunya -- perubahan kontrak protokol
 * (format token, kebijakan status publik, dsb.) di satu sisi tidak otomatis konsisten dengan
 * sisi lain.</p>
 *
 * <p><b>Catatan keamanan:</b> setiap permintaan dibatasi laju lewat {@link JurnalRateLimiter}
 * (maks. 600 permintaan/60 detik per IP, kunci {@code "oai"}). Hanya item berstatus publik
 * ({@link #PUBLIC_STATUSES}) dan aktif yang dapat dipanen, disaring pula per-tenant lewat {@link
 * RepositoryTenantScope#currentKey()} pada setiap query Hibernate. Resumption token (penanda
 * halaman lanjutan {@code ListIdentifiers}/{@code ListRecords}) ditandatangani HMAC-SHA256
 * ({@link #hmac}) dengan kunci rahasia {@link #TOKEN_SECRET} dan diverifikasi constant-time
 * ({@link MessageDigest#isEqual}) plus batas kedaluwarsa ({@link #tokenMaximumAgeMillis}) di
 * {@link #decodeToken}, sehingga token tidak dapat dipalsukan atau diputar ulang tanpa batas
 * waktu.</p>
 *
 * @see HttpServlet
 * @see Repository
 */
public class Oai extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** Jumlah record maksimum per halaman untuk verb {@code ListIdentifiers}/{@code ListRecords}; halaman berikutnya diambil lewat resumption token. */
    private static final int PAGE_SIZE = 100;
    /** Kunci rahasia HMAC-SHA256 untuk menandatangani/memverifikasi resumption token, diinisialisasi sekali lewat {@link #tokenSecret()}. */
    private static final byte[] TOKEN_SECRET = tokenSecret();
    /** Nilai {@code syncStatus} pada {@link RepoItem} yang dianggap "publik" dan boleh dipanen lewat OAI-PMH; status lain (draf, ditolak, dsb.) disembunyikan dari harvester. */
    private static final String[] PUBLIC_STATUSES =
        new String[] { "SYNCED", "PUBLISHED", "APPROVED" };

    /** Namespace XML protokol OAI-PMH 2.0. */
    private static final String OAI_NS =
        "http://www.openarchives.org/OAI/2.0/";
    /** Lokasi skema XSD protokol OAI-PMH 2.0, dirujuk pada atribut {@code xsi:schemaLocation} elemen akar. */
    private static final String OAI_SCHEMA =
        "http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd";
    /** Namespace elemen Dublin Core inti (dc:title, dc:creator, dst.); tidak dipakai langsung di file ini, dipertahankan sebagai referensi format. */
    private static final String DC_NS =
        "http://purl.org/dc/elements/1.1/";
    /** Namespace wrapper {@code oai_dc} pembungkus elemen Dublin Core dalam respons OAI-PMH. */
    private static final String OAI_DC_NS =
        "http://www.openarchives.org/OAI/2.0/oai_dc/";
    /** Lokasi skema XSD untuk format metadata {@code oai_dc}. */
    private static final String OAI_DC_SCHEMA =
        "http://www.openarchives.org/OAI/2.0/oai_dc.xsd";
    /** Layanan serialisasi/negosiasi format metadata (saat ini hanya {@code oai_dc}) yang dipakai {@link #handleListMetadataFormats}, {@link #handleGetRecord}, dan {@link #writeRecord}. */
    private final JurnalMetadataFormatService metadataFormats = new JurnalMetadataFormatService();

    /** Pola tanggal-waktu lengkap UTC ({@code datestamp} granularitas detik) sesuai spesifikasi OAI-PMH. */
    private static final String FMT_FULL = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    /** Pola tanggal saja (granularitas hari), dipakai bila argumen {@code from}/{@code until} klien memakai granularitas hari. */
    private static final String FMT_DAY  = "yyyy-MM-dd";

    /**
     * Membuat {@link SimpleDateFormat} baru bertimezone UTC untuk pola {@link #FMT_FULL},
     * non-lenient (menolak tanggal yang secara kalender tidak valid). Instance baru dibuat setiap
     * pemanggilan karena {@code SimpleDateFormat} tidak thread-safe.
     */
    private static SimpleDateFormat fullFmt() {
        SimpleDateFormat sdf = new SimpleDateFormat(FMT_FULL);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        sdf.setLenient(false);
        return sdf;
    }
    /**
     * Membuat {@link SimpleDateFormat} baru bertimezone UTC untuk pola {@link #FMT_DAY},
     * non-lenient. Instance baru dibuat setiap pemanggilan karena {@code SimpleDateFormat} tidak
     * thread-safe.
     */
    private static SimpleDateFormat dayFmt() {
        SimpleDateFormat sdf = new SimpleDateFormat(FMT_DAY);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        sdf.setLenient(false);
        return sdf;
    }

    /** Konstruktor default tanpa inisialisasi khusus. */
    public Oai() {
        super();
    }

    @Override
    /** Menangani permintaan {@code GET}: mendelegasikan seluruhnya ke {@link #process}, satu-satunya titik pemrosesan protokol OAI-PMH. */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        process(request, response);
    }

    @Override
    /** Menangani permintaan {@code POST}: mendelegasikan seluruhnya ke {@link #process}, identik dengan {@link #doGet} -- protokol OAI-PMH menerima argumen lewat GET maupun POST. */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        process(request, response);
    }

    // ── Main dispatcher ───────────────────────────────────────────────────────

    /**
     * Dispatcher utama protokol OAI-PMH: membatasi laju permintaan ({@link JurnalRateLimiter}, 600/menit
     * per IP), memasang header respons XML yang mengeraskan endpoint publik ini (cache dimatikan,
     * CORS terbuka untuk harvester, {@code X-Content-Type-Options}, {@code Content-Security-Policy}
     * ketat, {@code Referrer-Policy: no-referrer}), menuliskan pembuka {@code <OAI-PMH>} dan {@code
     * <responseDate>}, lalu meruting berdasarkan parameter {@code verb} ke salah satu {@code
     * handleXxx} yang sesuai -- {@code verb} kosong atau tidak dikenal menghasilkan error {@code
     * badVerb}. Menutup elemen {@code </OAI-PMH>} di akhir apa pun hasil verb-nya.
     */
    private void process(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        if(!JurnalRateLimiter.allow("oai",request.getRemoteAddr(),600,60000L)){response.sendError(429,"Too many OAI requests.");return;}

        /* setCharacterEncoding tidak ada di Servlet API lama (build server);
         * charset sudah ditetapkan lewat setContentType di bawah. */
        response.setContentType("text/xml; charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Content-Security-Policy",
            "default-src 'none'; frame-ancestors 'none'; base-uri 'none'");
        response.setHeader("Referrer-Policy", "no-referrer");

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

    /**
     * Menangani verb {@code Identify}: menuliskan metadata deskriptif repositori (nama dari
     * konfigurasi {@code oai_repository_name}/{@code nama_institusi}, email admin dari {@code
     * oai_admin_email}/{@code email_institusi}, tanggal record tertua lewat {@link
     * #queryEarliestDatestamp}, kebijakan {@code deletedRecord=transient}) sesuai skema {@code
     * oai-identifier} standar OAI-PMH.
     */
    private void handleIdentify(PrintWriter out, HttpServletRequest request, String baseUrl) {
        String repoName = System.getProperty("ais.repository.oaiRepositoryName", "").trim();
        if (repoName.isEmpty()) {
            repoName = Common.getKonfigurasi("oai_repository_name",
                    Common.getKonfigurasi("nama_institusi", "Repository").getNilai()).getNilai();
        }
        String adminEmail = System.getProperty("ais.repository.oaiAdminEmail", "").trim();
        if (adminEmail.isEmpty()) {
            adminEmail = Common.getKonfigurasi("oai_admin_email",
                    Common.getKonfigurasi("email_institusi", "admin@repository.ac.id").getNilai()).getNilai();
        }
        String earliest   = queryEarliestDatestamp();

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
        out.println("  </Identify>");
    }

    // ── Verb: ListMetadataFormats ─────────────────────────────────────────────

    /**
     * Menangani verb {@code ListMetadataFormats}: bila argumen {@code identifier} disertakan,
     * memvalidasi dulu bahwa record tersebut ada dan publik ({@link #findByIdentifier}) sebelum
     * menuliskan daftar format yang didukung ({@link #metadataFormats}, saat ini hanya {@code
     * oai_dc}); tanpa {@code identifier}, daftar format ditulis tanpa validasi record.
     */
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
        for (JurnalMetadataFormatService.Format format : metadataFormats.formats()) {
            out.println("    <metadataFormat>");
            out.println("      <metadataPrefix>" + escXml(format.prefix) + "</metadataPrefix>");
            out.println("      <schema>" + escXml(format.schema) + "</schema>");
            out.println("      <metadataNamespace>" + escXml(format.namespace) + "</metadataNamespace>");
            out.println("    </metadataFormat>");
        }
        out.println("  </ListMetadataFormats>");
    }

    // ── Verb: ListSets ────────────────────────────────────────────────────────

    /**
     * Menangani verb {@code ListSets}: menuliskan seluruh {@link RepoCollection} aktif milik
     * tenant saat ini ({@link RepositoryTenantScope#currentKey()}) sebagai {@code <set>}, diurutkan
     * berdasar {@code sortOrder} lalu nama. Mengembalikan error {@code noSetHierarchy} bila
     * repositori tidak punya koleksi sama sekali atau bila query gagal.
     */
    private void handleListSets(PrintWriter out) {
        Session session = null;
        try {
            session = openSession();
            @SuppressWarnings("unchecked")
            List<RepoCollection> cols = session.createCriteria(RepoCollection.class)
                .add(Restrictions.eq("tenantKey", RepositoryTenantScope.currentKey()))
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

    /**
     * Menangani verb {@code GetRecord}: memvalidasi argumen wajib {@code identifier} dan {@code
     * metadataPrefix} (harus {@code oai_dc}), mencari record publik lewat {@link
     * #findByIdentifier}, lalu menuliskan satu {@code <record>} lengkap ({@link #writeRecord}).
     * Mengembalikan {@code badArgument}/{@code cannotDisseminateFormat}/{@code idDoesNotExist}
     * sesuai kegagalan validasi masing-masing.
     */
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
        if (!metadataFormats.supports(metadataPrefix)) {
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
            writeRecord(out, session, item, metas, setSpec, metadataPrefix, request);
            out.println("  </GetRecord>");
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Oai.java:294");
            writeError(out, "badArgument", "Error retrieving record: " + escXml(e.getMessage()));
        } finally {
            closeSession(session);
        }
    }

    // ── Verb: ListIdentifiers ─────────────────────────────────────────────────

    /**
     * Menangani verb {@code ListIdentifiers}: mem-parsing argumen query/resumption token
     * ({@link #parseListQuery}), lalu menuliskan header ({@code identifier}/{@code
     * datestamp}/{@code setSpec}, ditandai {@code status="deleted"} bila item sudah ditarik
     * ({@code isWithdrawn})) untuk satu halaman record ({@link #PAGE_SIZE}) yang cocok. Menyertakan
     * resumption token ({@link #writeResumptionToken}) bila masih ada halaman berikutnya.
     * Mengembalikan {@code noRecordsMatch} bila tidak ada record yang cocok atau bila query gagal.
     */
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
                out.println("    <header" + (Boolean.TRUE.equals(item.getIsWithdrawn()) ? " status=\"deleted\"" : "") + ">");
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

    /**
     * Menangani verb {@code ListRecords}: sama seperti {@link #handleListIdentifiers} tapi
     * menuliskan record lengkap (header + metadata Dublin Core, lewat {@link #writeRecord}) untuk
     * satu halaman ({@link #PAGE_SIZE}), bukan hanya header identitasnya.
     */
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
                writeRecord(out, session, item, metas, setSpec, q.metadataPrefix, request);
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

    /**
     * Menuliskan satu elemen {@code <record>} OAI-PMH: header ({@code identifier}/{@code
     * datestamp}/{@code setSpec}, ditandai {@code status="deleted"} bila item ditarik) selalu
     * ditulis; elemen {@code <metadata>} (hasil {@link JurnalMetadataFormatService#serialize})
     * HANYA ditulis untuk record yang belum ditarik -- protokol OAI-PMH mewajibkan record yang
     * dihapus diumumkan sebagai header {@code deleted} tanpa metadata, agar pemanen dapat
     * menyinkronkan penghapusan tanpa mengetahui isi record sebelumnya.
     */
    private void writeRecord(PrintWriter out, Session session, RepoItem item,
                              List<RepoItemMetadata> metas, String setSpec,
                              String metadataPrefix, HttpServletRequest request) {
        String datestamp = itemDatestamp(item);
        boolean deleted  = Boolean.TRUE.equals(item.getIsWithdrawn());

        out.println("    <record>");
        out.println("      <header" + (deleted ? " status=\"deleted\"" : "") + ">");
        out.println("        <identifier>" + escXml(item.getOaiIdentifier()) + "</identifier>");
        out.println("        <datestamp>" + datestamp + "</datestamp>");
        out.println("        <setSpec>" + setSpec + "</setSpec>");
        out.println("      </header>");

        if (!deleted) {
            out.println("      <metadata>");
            out.println("        " + metadataFormats.serialize(metadataPrefix, item, metas,
                    buildPublicItemUrl(request, item.getId())));
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

    /**
     * Mengumpulkan seluruh pengenal ({@code dc:identifier}) yang tersedia untuk {@code item}:
     * OAI identifier utama dan, bila ada, URI handle DSpace ({@code https://hdl.handle.net/...}).
     */
    private List<String> buildIdentifiers(RepoItem item) {
        List<String> ids = new ArrayList<String>();
        if (item.getOaiIdentifier() != null && !item.getOaiIdentifier().isEmpty())
            ids.add(item.getOaiIdentifier());
        if (item.getDspaceHandle() != null && !item.getDspaceHandle().isEmpty())
            ids.add("https://hdl.handle.net/" + item.getDspaceHandle());
        return ids;
    }

    // ── Query helpers ─────────────────────────────────────────────────────────

    /**
     * Membangun kriteria Hibernate dasar yang dipakai bersama oleh {@link #listItems} dan {@link
     * #countItems}: item aktif, milik tenant saat ini ({@link RepositoryTenantScope#currentKey()}),
     * berstatus publik ({@link #PUBLIC_STATUSES}), lalu menambahkan filter {@code setSpec} (koleksi,
     * bila diberikan) dan rentang tanggal {@code from}/{@code until} (berdasarkan {@code
     * lastSyncAt}) dari {@code q}.
     */
    private Criteria buildItemCriteria(Session session, ListQuery q) {
        Criteria c = session.createCriteria(RepoItem.class);
        // Only records that reached a public state may be harvested. A
        // withdrawn public record remains visible as an OAI deleted header.
        c.add(Restrictions.eq("aktif", Boolean.TRUE));
        c.add(Restrictions.eq("tenantKey", RepositoryTenantScope.currentKey()));
        c.add(Restrictions.in("syncStatus", PUBLIC_STATUSES));

        if (q.setSpec != null) {
            // setSpec format: col_<id>
            try {
                Long colId = Long.parseLong(q.setSpec.replaceFirst("^col_", ""));
                c.add(Restrictions.eq("collectionId", colId));
            } catch (NumberFormatException ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Oai.java:522");}
        }
        if (q.from != null)
            c.add(Restrictions.ge("lastSyncAt", q.from));
        if (q.until != null)
            c.add(Restrictions.le("lastSyncAt", q.until));

        return c;
    }

    @SuppressWarnings("unchecked")
    /** Mengambil satu halaman {@link RepoItem} sesuai {@code q} ({@link #buildItemCriteria}), diurutkan menaik berdasar id, dimulai dari {@code offset} sebanyak {@code limit} baris. */
    private List<RepoItem> listItems(Session session, ListQuery q, int offset, int limit) {
        Criteria c = buildItemCriteria(session, q);
        c.addOrder(Order.asc("id"));
        c.setFirstResult(offset);
        c.setMaxResults(limit);
        return c.list();
    }

    /** Menghitung total record yang cocok dengan {@code q} ({@link #buildItemCriteria}), dipakai untuk {@code completeListSize} pada resumption token dan menentukan apakah masih ada halaman berikutnya. */
    private long countItems(Session session, ListQuery q) {
        Criteria c = buildItemCriteria(session, q);
        c.setProjection(Projections.rowCount());
        Object result = c.uniqueResult();
        return result == null ? 0L : ((Number) result).longValue();
    }

    @SuppressWarnings("unchecked")
    /** Memuat seluruh baris metadata EAV ({@link RepoItemMetadata}) aktif milik {@code itemId}, diurutkan berdasar nama field lalu urutan tampil ({@code place}), untuk digabung ke Dublin Core lewat {@link #buildDublinCore}. */
    private List<RepoItemMetadata> loadMetadata(Session session, Long itemId) {
        return session.createCriteria(RepoItemMetadata.class)
            .add(Restrictions.eq("itemId", itemId))
            .add(Restrictions.eq("aktif", Boolean.TRUE))
            .addOrder(Order.asc("metadataField"))
            .addOrder(Order.asc("place"))
            .list();
    }

    /**
     * Mencari satu {@link RepoItem} publik berdasarkan {@code oaiIdentifier}-nya, dibatasi ke
     * tenant saat ini ({@link RepositoryTenantScope#currentKey()}) dan status publik ({@link
     * #PUBLIC_STATUSES}). Item non-publik atau milik tenant lain tidak akan pernah ditemukan lewat
     * method ini, walau id-nya valid di database.
     *
     * @return item yang cocok, atau {@code null} bila tidak ditemukan/terjadi galat
     */
    private RepoItem findByIdentifier(String identifier) {
        Session session = null;
        try {
            session = openSession();
            return (RepoItem) session.createCriteria(RepoItem.class)
                .add(Restrictions.eq("oaiIdentifier", identifier))
                .add(Restrictions.eq("tenantKey", RepositoryTenantScope.currentKey()))
                .add(Restrictions.eq("aktif", Boolean.TRUE))
                .add(Restrictions.in("syncStatus", PUBLIC_STATUSES))
                .setMaxResults(1)
                .uniqueResult();
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Oai.java:567");
            return null;
        } finally {
            closeSession(session);
        }
    }

    /**
     * Mencari {@code lastSyncAt} paling awal di antara item publik aktif milik tenant saat ini,
     * dipakai sebagai {@code <earliestDatestamp>} pada respons verb {@code Identify}.
     *
     * @return datestamp terformat {@link #FMT_FULL}, atau {@code "2000-01-01T00:00:00Z"} sebagai
     *         fallback bila tidak ada data atau terjadi galat
     */
    private String queryEarliestDatestamp() {
        Session session = null;
        try {
            session = openSession();
            Object result = session.createCriteria(RepoItem.class)
                .add(Restrictions.eq("aktif", Boolean.TRUE))
                .add(Restrictions.eq("tenantKey", RepositoryTenantScope.currentKey()))
                .add(Restrictions.in("syncStatus", PUBLIC_STATUSES))
                .add(Restrictions.isNotNull("lastSyncAt"))
                .setProjection(Projections.min("lastSyncAt"))
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

    /**
     * Menuliskan elemen {@code <resumptionToken>} bila masih ada halaman berikutnya (token
     * ditandatangani lewat {@link #encodeToken}), atau token kosong ({@code cursor} tanpa isi)
     * pada halaman terakhir dari sebuah rangkaian ber-halaman-banyak untuk menandai selesai;
     * tidak menulis apa pun bila hasil query hanya satu halaman.
     */
    private void writeResumptionToken(PrintWriter out, ListQuery q,
                                       long total, int fetched) {
        int nextOffset = q.offset + fetched;
        boolean hasMore = nextOffset < total;

        if (hasMore) {
            String token = encodeToken(q.from, q.until, q.setSpec, nextOffset, total, q.metadataPrefix);
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
                                 int offset, long total, String metadataPrefix) {
        SimpleDateFormat _f = fullFmt();
        String f = from  != null ? _f.format(from)  : "";
        String u = until != null ? _f.format(until) : "";
        String s = set   != null ? set : "";
        String raw = f + "|" + u + "|" + s + "|" + offset + "|" + total + "|" + metadataPrefix + "|" + System.currentTimeMillis();
        String signed = raw + "|" + hex(hmac(raw));
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(signed.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Membongkar dan memvalidasi resumption token yang dihasilkan {@link #encodeToken}: memverifikasi
     * tanda tangan HMAC-SHA256 secara constant-time ({@link MessageDigest#isEqual}), menolak token
     * yang kedaluwarsa atau diterbitkan di masa depan (celah toleransi 60 detik, batas umur {@link
     * #tokenMaximumAgeMillis()}), lalu memvalidasi rentang {@code offset}/{@code total} masuk akal
     * (0..1.000.000 dan 0..1.000.000.000, {@code offset<=total}) sebelum membangun ulang {@link
     * ListQuery}-nya.
     *
     * @return query hasil dekode, atau {@code null} bila token tidak valid/rusak/kedaluwarsa/dipalsukan
     */
    private ListQuery decodeToken(String token) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(token);
            String raw = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|", -1);
            if (parts.length != 8) return null;
            String unsigned = raw.substring(0, raw.lastIndexOf('|'));
            if (!MessageDigest.isEqual(hmac(unsigned), unhex(parts[7]))) return null;
            long issuedAt=Long.parseLong(parts[6]);
            if (issuedAt > System.currentTimeMillis()+60000L || System.currentTimeMillis()-issuedAt > tokenMaximumAgeMillis()) return null;

            ListQuery q = new ListQuery();
            q.from   = parts[0].isEmpty() ? null : parseDateFlexible(parts[0]);
            q.until  = parts[1].isEmpty() ? null : parseDateFlexible(parts[1]);
            q.setSpec = parts[2].isEmpty() ? null : parts[2];
            q.offset = Integer.parseInt(parts[3]);
            long total=Long.parseLong(parts[4]);
            if(q.offset<0||q.offset>1000000||total<0||total>1000000000L||q.offset>total)return null;
            q.metadataPrefix = parts.length > 5 && metadataFormats.supports(parts[5]) ? parts[5] : "oai_dc";
            return q;
        } catch (Exception e) {
            return null;
        }
    }

    // ── ListQuery parser ──────────────────────────────────────────────────────

    /**
     * Mem-parsing argumen verb {@code ListIdentifiers}/{@code ListRecords} menjadi {@link
     * ListQuery}: bila {@code resumptionToken} disertakan, argumen lain (selain {@code verb}) tidak
     * boleh ada (dilanggar -> {@code badArgument}) dan query dibangun ulang lewat {@link
     * #decodeToken} (token tidak valid -> {@code badResumptionToken}); tanpa token, memvalidasi
     * {@code metadataPrefix} wajib ada dan didukung, granularitas {@code from}/{@code until} harus
     * sama (keduanya tanggal saja atau keduanya tanggal-waktu), serta {@code set} harus berpola
     * {@code col_<angka>} bila diberikan.
     *
     * @return query siap pakai, atau {@link ListQuery} dengan {@code error}/{@code errorCode} terisi
     *         bila validasi gagal (pemanggil wajib mengecek {@code q.error != null} sebelum query)
     */
    private ListQuery parseListQuery(HttpServletRequest request) {
        ListQuery q = new ListQuery();
        String token = param(request, "resumptionToken");

        if (token != null && !token.trim().isEmpty()) {
            // When resumptionToken is provided, no other args allowed (except verb)
            if (param(request, "metadataPrefix") != null || param(request, "from") != null
                    || param(request, "until") != null || param(request, "set") != null
                    || param(request, "identifier") != null) {
                q.errorCode = "badArgument";
                q.error = "resumptionToken must be the only argument besides verb.";
                return q;
            }
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
        if (!metadataFormats.supports(q.metadataPrefix.trim())) {
            q.errorCode = "cannotDisseminateFormat";
            q.error = "Metadata format not supported: " + escXml(q.metadataPrefix);
            return q;
        }

        String fromStr  = param(request, "from");
        String untilStr = param(request, "until");
        String setSpec  = param(request, "set");

        if (fromStr != null && untilStr != null
                && ((fromStr.length() == 10) != (untilStr.length() == 10))) {
            q.errorCode = "badArgument";
            q.error = "Arguments 'from' and 'until' must use the same granularity.";
            return q;
        }

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
        if (q.setSpec != null && !q.setSpec.matches("col_[1-9][0-9]*")) {
            q.errorCode = "noRecordsMatch";
            q.error = "Unknown set: " + escXml(q.setSpec);
            return q;
        }
        q.offset  = 0;
        return q;
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /**
     * Tipe implementasi bersarang {@link ListQuery} milik {@link Oai}. Kelas ini memberi nama pada state atau
     * perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link Oai}. Dependensi yang
     * diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini merupakan detail
     * implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String metadataPrefix}, {@code Date
     * from}, {@code Date until}, {@code String setSpec}, {@code int offset}, {@code String error}, {@code String
     * errorCode}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see Oai
     */
    private static class ListQuery {
        /** Format metadata yang diminta klien (harus {@code oai_dc}); pada token hasil {@link #decodeToken} nilai ini dipulihkan dari token. */
        String metadataPrefix;
        /** Batas bawah rentang {@code lastSyncAt} ({@code null} = tanpa batas bawah). */
        Date   from;
        /** Batas atas rentang {@code lastSyncAt} ({@code null} = tanpa batas atas). */
        Date   until;
        /** Filter koleksi berpola {@code col_<id>} ({@code null} = semua koleksi). */
        String setSpec;
        /** Posisi baris pertama pada halaman saat ini (0 untuk permintaan awal, dipulihkan dari resumption token untuk halaman lanjutan). */
        int    offset;
        /** Pesan kesalahan manusiawi bila parsing/validasi gagal; {@code null} berarti query valid. */
        String error;
        /** Kode error OAI-PMH ({@code badArgument}, {@code badResumptionToken}, dst.) yang menyertai {@link #error}. */
        String errorCode = "badArgument";
    }

    /**
     * Menentukan URL dasar ({@code baseURL}) endpoint OAI-PMH ini: memakai {@code
     * ais.repository.publicBaseUrl} bila dikonfigurasi (divalidasi ketat -- skema http/https,
     * host wajib, tanpa userinfo/query/fragment, path kosong atau {@code "/"} saja) agar tidak
     * disalahgunakan sebagai open redirect/URL sembarang, atau menyusunnya dari skema/host/port
     * permintaan saat ini sebagai fallback (host divalidasi format hostname/alamat IP).
     *
     * @throws IllegalStateException bila konfigurasi maupun origin permintaan tidak valid
     */
    private String buildBaseUrl(HttpServletRequest request) {
        String configured = System.getProperty("ais.repository.publicBaseUrl", "").trim();
        if (!configured.isEmpty()) {
            try {
                URL url = new URL(configured);
                String protocol = url.getProtocol();
                String path = url.getPath() == null ? "" : url.getPath().trim();
                if (!("http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol))
                        || url.getHost() == null || url.getHost().trim().isEmpty()
                        || url.getUserInfo() != null || url.getQuery() != null || url.getRef() != null
                        || !(path.isEmpty() || "/".equals(path))) {
                    throw new IllegalStateException("ais.repository.publicBaseUrl tidak valid.");
                }
                String host = url.getHost().indexOf(':') >= 0 ? "[" + url.getHost() + "]" : url.getHost();
                return protocol.toLowerCase() + "://" + host + (url.getPort() < 0 ? "" : ":" + url.getPort())
                        + request.getContextPath() + "/oai";
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException("ais.repository.publicBaseUrl tidak valid.", e);
            }
        }

        String scheme = request.getScheme() == null ? "" : request.getScheme().trim().toLowerCase();
        String host = request.getServerName() == null ? "" : request.getServerName().trim();
        int port = request.getServerPort();
        if (!("http".equals(scheme) || "https".equals(scheme)) || host.isEmpty()
                || !host.matches("[A-Za-z0-9.-]+|[0-9a-fA-F:]+") || port < 1 || port > 65535) {
            throw new IllegalStateException("Origin publik OAI tidak valid.");
        }
        String authority = host.indexOf(':') >= 0 ? "[" + host + "]" : host;
        boolean defaultPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
        return scheme + "://" + authority + (defaultPort ? "" : ":" + port)
                + request.getContextPath() + "/oai";
    }

    /** Menyusun URL publik halaman detail item ({@code /repository/item/<id>}), diturunkan dari {@link #buildBaseUrl} dengan akhiran {@code /oai} dipangkas; dirujuk sebagai identifier tautan pada metadata Dublin Core (elemen {@code dc:identifier} tambahan lewat serializer). */
    private String buildPublicItemUrl(HttpServletRequest request, Long itemId) {
        String oaiBaseUrl = buildBaseUrl(request);
        return oaiBaseUrl.substring(0, oaiBaseUrl.length() - "/oai".length())
                + "/repository/item/" + itemId;
    }

    /**
     * Menentukan pengenal domain repositori untuk skema {@code oai-identifier} (dipakai
     * {@link #handleIdentify}): memakai konfigurasi {@code oai_repository_identifier} bila
     * formatnya valid (menyerupai domain, huruf kecil), jatuh ke nama host permintaan bila tidak,
     * dan ke {@code "repository.localhost"} sebagai upaya terakhir bila keduanya tidak valid.
     */
    private String buildRepositoryIdentifier(HttpServletRequest request) {
        String configured = Common.getKonfigurasi("oai_repository_identifier", "").getNilai();
        String value = configured == null ? "" : configured.trim().toLowerCase();
        if (!value.matches("[a-z0-9]+([.-][a-z0-9]+)+")) {
            value = request.getServerName() == null ? "" : request.getServerName().trim().toLowerCase();
            if (!value.matches("[a-z0-9]+([.-][a-z0-9]+)+")) value = "repository." + (value.matches("[a-z0-9]+") ? value : "localhost");
        }
        return value;
    }

    /**
     * Menentukan {@code datestamp} OAI-PMH untuk {@code item}: {@code lastSyncAt} bila ada,
     * jatuh ke {@code submittedAt}, lalu {@code tanggal_dirubah}; waktu saat ini sebagai upaya
     * terakhir bila ketiganya kosong.
     */
    private String itemDatestamp(RepoItem item) {
        Date d = item.getLastSyncAt() != null ? item.getLastSyncAt()
               : item.getSubmittedAt() != null ? item.getSubmittedAt()
               : item.getTanggal_dirubah();
        SimpleDateFormat f = fullFmt();
        return d != null ? f.format(d) : f.format(new Date());
    }

    /** Memformat {@code d} dengan pola tanggal-saja {@link #FMT_DAY}, atau string kosong bila {@code null}. */
    private String formatDate(Date d) {
        if (d == null) return "";
        return dayFmt().format(d);
    }

    /** Mem-parsing tanggal dengan granularitas fleksibel: 10 karakter ditafsirkan sebagai tanggal-saja ({@link #FMT_DAY}), selain itu sebagai tanggal-waktu lengkap ({@link #FMT_FULL}). */
    private Date parseDateFlexible(String s) throws ParseException {
        if (s.length() == 10)
            return dayFmt().parse(s);
        return fullFmt().parse(s);
    }

    /** Mem-parsing argumen {@code until} bergranularitas hari sebagai akhir hari tersebut (23:59:59 UTC) agar rentang tanggal inklusif; tanggal-waktu lengkap diteruskan apa adanya lewat {@link #parseDateFlexible}. */
    private Date parseDateUntilEndOfDay(String s) throws ParseException {
        if (s.length() == 10)
            return fullFmt().parse(s + "T23:59:59Z");
        return parseDateFlexible(s);
    }

    /** Membungkus {@code val} menjadi list satu elemen (dipangkas spasi), atau list kosong bila {@code null}/kosong -- bentuk seragam yang dibutuhkan {@link #buildDublinCore} untuk elemen Dublin Core bernilai tunggal. */
    private List<String> listOf(String val) {
        List<String> list = new ArrayList<String>();
        if (val != null && !val.trim().isEmpty())
            list.add(val.trim());
        return list;
    }

    /** Memecah {@code val} menjadi beberapa elemen berdasarkan baris baru/koma/titik-koma (dipakai untuk field multi-nilai seperti penulis/subjek), membuang bagian yang kosong setelah dipangkas. */
    private List<String> splitLines(String val) {
        List<String> list = new ArrayList<String>();
        if (val == null || val.trim().isEmpty()) return list;
        for (String part : val.split("[\\n;,]+")) {
            String t = part.trim();
            if (!t.isEmpty()) list.add(t);
        }
        return list;
    }

    /** Membaca parameter permintaan {@code name}, mengembalikan {@code null} (bukan string kosong) bila tidak ada atau hanya berisi spasi -- menyeragamkan pengecekan "argumen tidak diberikan" di seluruh handler verb. */
    private String param(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        return v == null ? null : v.trim().isEmpty() ? null : v.trim();
    }

    /** Meng-escape lima karakter spesial XML ({@code & < > " '}) pada {@code s} agar aman disisipkan sebagai teks/atribut elemen; mengembalikan string kosong bila {@code s} {@code null}. */
    private String escXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Menuliskan elemen {@code <request>} yang mengulang kembali argumen permintaan (verb dan
     * argumen dikenal lainnya, di-escape XML) beserta URL dasar endpoint, sesuai wajib protokol
     * OAI-PMH agar pemanen dapat mengonfirmasi permintaan yang benar-benar diproses server.
     */
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

    /** Menuliskan elemen {@code <error code="...">} standar OAI-PMH; kode dan pesan di-escape XML. */
    private void writeError(PrintWriter out, String code, String message) {
        out.println("  <error code=\"" + escXml(code) + "\">" + escXml(message) + "</error>");
    }

    // ── Hibernate helpers ─────────────────────────────────────────────────────

    /** Membuka sesi Hibernate baru lewat {@link HibernateUtil#openSession()} untuk satu operasi baca metadata repositori. */
    private Session openSession() {
        return HibernateUtil.openSession();
    }

    /**
     * Menentukan kunci rahasia HMAC untuk resumption token, dengan urutan prioritas: properti
     * sistem {@code ais.repository.oaiTokenSecret}, lalu env var {@code
     * AIS_REPOSITORY_OAI_TOKEN_SECRET}, lalu env var legacy {@code AIS_JURNAL_OAI_TOKEN_SECRET} --
     * dipakai bila panjangnya minimal 32 karakter. Bila tidak ada satu pun yang memenuhi syarat,
     * dibangkitkan kunci acak 32 byte ({@link SecureRandom}) yang hanya berlaku selama proses JVM
     * berjalan -- resumption token yang diterbitkan sebelum restart tidak akan valid lagi setelahnya,
     * sehingga disarankan mengonfigurasi kunci tetap di lingkungan produksi multi-instance.
     */
    private static byte[] tokenSecret(){
        String configured=System.getProperty("ais.repository.oaiTokenSecret","").trim();
        if(configured.length()<32){String env=System.getenv("AIS_REPOSITORY_OAI_TOKEN_SECRET");configured=env==null?"":env.trim();}
        if(configured.length()<32){String legacy=System.getenv("AIS_JURNAL_OAI_TOKEN_SECRET");configured=legacy==null?"":legacy.trim();}
        if(configured.length()>=32)return configured.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] random=new byte[32];new SecureRandom().nextBytes(random);return random;
    }
    /**
     * Batas umur maksimum resumption token dalam milidetik, dari properti sistem {@code
     * ais.repository.oaiTokenTtlSeconds} (default 86400 detik/1 hari), dijepit ke rentang
     * 300..604800 detik (5 menit..7 hari). Fallback 86400000 ms bila konfigurasi tidak valid.
     */
    private static long tokenMaximumAgeMillis(){
        try{
            long seconds=Long.parseLong(System.getProperty("ais.repository.oaiTokenTtlSeconds","86400"));
            return Math.max(300L,Math.min(seconds,604800L))*1000L;
        }catch(Exception e){return 86400000L;}
    }
    /** Menghitung HMAC-SHA256 {@code value} memakai kunci {@link #TOKEN_SECRET}; melempar {@link IllegalStateException} bila algoritma tak tersedia (seharusnya tidak pernah terjadi di JVM standar). */
    private static byte[] hmac(String value){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(TOKEN_SECRET,"HmacSHA256"));return mac.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(Exception e){throw new IllegalStateException(e);}}
    /** Mengonversi {@code value} ke representasi heksadesimal huruf kecil (dipakai untuk menyisipkan HMAC ke dalam token teks). */
    private static String hex(byte[] value){StringBuilder b=new StringBuilder();for(byte x:value)b.append(String.format("%02x",x&255));return b.toString();}
    /** Mengonversi heksadesimal {@code value} kembali ke {@code byte[]}; mengembalikan array kosong bila panjang/format tidak sesuai 64 karakter hex (bukan melempar exception), sehingga token yang dirusak gagal verifikasi tanda tangan secara aman alih-alih error tak tertangani. */
    private static byte[] unhex(String value){if(value==null||!value.matches("(?i)[0-9a-f]{64}"))return new byte[0];byte[]out=new byte[32];for(int i=0;i<32;i++)out[i]=(byte)Integer.parseInt(value.substring(i*2,i*2+2),16);return out;}

    /** Menutup {@code session} dengan aman (tidak melempar bila {@code null}/sudah tertutup) lewat {@link HibernateUtil#closeSessionQuietly}. */
    private void closeSession(Session session) {
        HibernateUtil.closeSessionQuietly(session);
    }
}
