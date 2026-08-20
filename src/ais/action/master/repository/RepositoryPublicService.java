package ais.action.master.repository;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.repository.RepoBitstream;
import ais.database.model.repository.RepoCollection;
import ais.database.model.repository.RepoItem;
import ais.database.model.repository.RepoItemMetadata;
import ais.database.model.file.LampiranLain;

/**
 * Read-only, typed service for the public repository.
 *
 * <p>The legacy page sent SQL and entity class names from JavaScript.  This
 * service deliberately accepts only a small query object and creates all
 * predicates on the server.  It also applies the public visibility policy in
 * one place so home, search, detail, citation, OAI and downloads cannot drift.</p>
 */
public class RepositoryPublicService {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 50;
    private static final String[] PUBLIC_STATUSES = new String[] { "SYNCED", "PUBLISHED", "APPROVED" };

    public static class Query {
        public String keyword = "";
        public Long collectionId;
        public String documentType = "";
        public String accessPolicy = "";
        public Integer year;
        public String sort = "newest";
        public int page = 1;
        public int pageSize = DEFAULT_PAGE_SIZE;
    }

    public static class Summary {
        public long totalItems;
        public long totalCollections;
        public long openAccess;
        public long metadataOnly;
    }

    public static class CollectionView {
        public Long id;
        public String kode;
        public String nama;
        public String deskripsi;
        public String tipe;
        public long itemCount;
    }

    public static class ItemCard {
        public Long id;
        public String oaiIdentifier;
        public String dspaceHandle;
        public String title;
        public String authors;
        public String abstractText;
        public String subjects;
        public String documentType;
        public String accessPolicy;
        public String language;
        public String publisher;
        public Date issuedAt;
        public String year;
        public Long collectionId;
        public String collectionName;
    }

    public static class BitstreamView {
        public Long id;
        public String namaFile;
        public String mimeType;
        public Long ukuranByte;
        public String checksum;
        public String description;
        public String accessPolicy;
        public boolean primaryFile;
    }

    public static class ItemDetail extends ItemCard {
        public Map<String, List<String> > metadata = new LinkedHashMap<String, List<String> >();
        public List<BitstreamView> files = new ArrayList<BitstreamView>();
    }

    public static class SearchResult {
        public Query query;
        public long total;
        public int totalPages;
        public List<ItemCard> items = new ArrayList<ItemCard>();
        public Map<String, Long> typeFacets = new LinkedHashMap<String, Long>();
        public Map<String, Long> accessFacets = new LinkedHashMap<String, Long>();
        public List<CollectionView> collections = new ArrayList<CollectionView>();
    }

    public Session session() {
        Session session = HibernateUtil.currentSession();
        if (session == null || !session.isOpen()) {
            session = HibernateUtil.currentNativeSession();
        }
        return session;
    }

    public Query normalize(Query input) {
        Query q = input == null ? new Query() : input;
        q.keyword = limit(clean(q.keyword), 200);
        q.documentType = limit(clean(q.documentType), 80);
        q.accessPolicy = normalizeAccess(clean(q.accessPolicy));
        q.sort = normalizeSort(q.sort);
        if (q.page < 1) q.page = 1;
        if (q.pageSize < 1) q.pageSize = DEFAULT_PAGE_SIZE;
        if (q.pageSize > MAX_PAGE_SIZE) q.pageSize = MAX_PAGE_SIZE;
        if (q.year != null && (q.year.intValue() < 1000 || q.year.intValue() > 3000)) q.year = null;
        if (q.collectionId != null && q.collectionId.longValue() <= 0L) q.collectionId = null;
        return q;
    }

    public Summary loadSummary() {
        Session session = session();
        Summary summary = new Summary();
        summary.totalItems = count(publicCriteria(session, null));
        summary.totalCollections = count(session.createCriteria(RepoCollection.class)
                .add(activeRestriction()));
        summary.openAccess = count(publicCriteria(session, null)
                .add(Restrictions.eq("accessPolicy", "OPEN_ACCESS")));
        summary.metadataOnly = count(publicCriteria(session, null)
                .add(Restrictions.eq("accessPolicy", "METADATA_ONLY")));
        return summary;
    }

    @SuppressWarnings("unchecked")
    public SearchResult search(Query input) {
        Query q = normalize(input);
        Session session = session();
        SearchResult result = new SearchResult();
        result.query = q;
        result.total = count(searchCriteria(session, q));
        result.totalPages = result.total == 0L ? 0 : (int) ((result.total + q.pageSize - 1L) / q.pageSize);
        if (result.totalPages > 0 && q.page > result.totalPages) q.page = result.totalPages;

        Criteria rows = searchCriteria(session, q);
        applySort(rows, q.sort);
        rows.setFirstResult((q.page - 1) * q.pageSize);
        rows.setMaxResults(q.pageSize);
        List<RepoItem> entities = rows.list();

        Map<Long, RepoCollection> collectionMap = loadCollectionMap(session, entities);
        for (int i = 0; i < entities.size(); i++) {
            RepoItem entity = entities.get(i);
            result.items.add(toCard(entity, collectionMap.get(entity.getCollectionId())));
        }
        result.typeFacets = groupFacet(session, q, "documentType");
        result.accessFacets = groupFacet(session, q, "accessPolicy");
        result.collections = listCollections(100);
        return result;
    }

    public List<ItemCard> latest(int maximum) {
        Query q = new Query();
        q.pageSize = maximum < 1 ? 6 : Math.min(maximum, MAX_PAGE_SIZE);
        q.sort = "newest";
        return search(q).items;
    }

    @SuppressWarnings("unchecked")
    public List<CollectionView> listCollections(int maximum) {
        Session session = session();
        List<RepoCollection> rows = session.createCriteria(RepoCollection.class)
                .add(activeRestriction())
                .addOrder(Order.asc("sortOrder"))
                .addOrder(Order.asc("nama"))
                .setMaxResults(maximum < 1 ? 100 : Math.min(maximum, 500))
                .list();
        Map<Long, Long> counts = collectionCounts(session);
        List<CollectionView> result = new ArrayList<CollectionView>();
        for (int i = 0; i < rows.size(); i++) {
            RepoCollection row = rows.get(i);
            CollectionView view = new CollectionView();
            view.id = row.getId();
            view.kode = safe(row.getKode());
            view.nama = safe(row.getNama());
            view.deskripsi = safe(row.getDeskripsi());
            view.tipe = safe(row.getTipe());
            Long itemCount = counts.get(row.getId());
            view.itemCount = itemCount == null ? 0L : itemCount.longValue();
            result.add(view);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public ItemDetail findPublicItem(Long id) {
        if (id == null || id.longValue() <= 0L) return null;
        Session session = session();
        RepoItem entity = (RepoItem) session.createCriteria(RepoItem.class)
                .add(Restrictions.eq("id", id))
                .add(publicVisibilityRestriction())
                .uniqueResult();
        if (entity == null) return null;

        RepoCollection collection = (RepoCollection) session.get(RepoCollection.class, entity.getCollectionId());
        ItemCard card = toCard(entity, collection);
        ItemDetail detail = new ItemDetail();
        copyCard(card, detail);

        List<RepoItemMetadata> metadataRows = session.createCriteria(RepoItemMetadata.class)
                .add(Restrictions.eq("itemId", id))
                .add(activeRestriction())
                .addOrder(Order.asc("place"))
                .addOrder(Order.asc("id"))
                .list();
        for (int i = 0; i < metadataRows.size(); i++) {
            RepoItemMetadata metadata = metadataRows.get(i);
            String field = clean(metadata.getMetadataField());
            if (!isPublicMetadataField(field)) continue;
            List<String> values = detail.metadata.get(field);
            if (values == null) {
                values = new ArrayList<String>();
                detail.metadata.put(field, values);
            }
            values.add(safe(metadata.getMetadataValue()));
        }

        List<RepoBitstream> bitstreams = session.createCriteria(RepoBitstream.class)
                .add(Restrictions.eq("itemId", id))
                .add(activeRestriction())
                .add(Restrictions.eq("accessPolicy", "OPEN_ACCESS"))
                .addOrder(Order.desc("primaryFile"))
                .addOrder(Order.asc("id"))
                .list();
        for (int i = 0; i < bitstreams.size(); i++) {
            RepoBitstream bitstream = bitstreams.get(i);
            if (!canDownload(entity, bitstream)) continue;
            detail.files.add(toBitstream(bitstream));
        }
        return detail;
    }

    public ItemDetail findPublicItemByOai(String identifier) {
        String value = limit(clean(identifier), 255);
        if (value.length() == 0) return null;
        RepoItem item = (RepoItem) session().createCriteria(RepoItem.class)
                .add(Restrictions.eq("oaiIdentifier", value))
                .add(publicVisibilityRestriction())
                .uniqueResult();
        return item == null ? null : findPublicItem(item.getId());
    }

    public RepoBitstream findDownloadableBitstream(Long id) {
        if (id == null || id.longValue() <= 0L) return null;
        Session session = session();
        RepoBitstream bitstream = (RepoBitstream) session.createCriteria(RepoBitstream.class)
                .add(Restrictions.eq("id", id))
                .add(activeRestriction())
                .add(Restrictions.eq("accessPolicy", "OPEN_ACCESS"))
                .uniqueResult();
        if (bitstream == null) return null;
        RepoItem item = (RepoItem) session.createCriteria(RepoItem.class)
                .add(Restrictions.eq("id", bitstream.getItemId()))
                .add(publicVisibilityRestriction())
                .add(Restrictions.eq("accessPolicy", "OPEN_ACCESS"))
                .uniqueResult();
        return canDownload(item, bitstream) ? bitstream : null;
    }

    @SuppressWarnings("unchecked")
    public File resolveBitstreamFile(RepoBitstream bitstream) {
        if (bitstream == null) return null;
        String storedPath = clean(bitstream.getPathSistem());
        if (storedPath.length() > 0) {
            File direct = new File(storedPath);
            if (direct.isAbsolute() && direct.exists() && direct.isFile()) return direct;
        }
        try {
            List<LampiranLain> attachments = session().createCriteria(LampiranLain.class)
                    .add(Restrictions.eq("ref", bitstream.getId()))
                    .add(Restrictions.eq("jenis", RepoBitstream.class.getName()))
                    .add(activeRestriction())
                    .addOrder(Order.desc("id"))
                    .setMaxResults(1)
                    .list();
            if (attachments.isEmpty()) return null;
            File file = attachments.get(0).ambilFile();
            return file != null && file.exists() && file.isFile() ? file : null;
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "RepositoryPublicService.resolveBitstreamFile");
            return null;
        }
    }

    public String citation(ItemDetail item, String format) {
        if (item == null) return "";
        String type = clean(format).toLowerCase();
        String year = safe(item.year);
        if ("bibtex".equals(type)) {
            return "@misc{ais_repository_" + item.id + ",\n"
                    + "  title = {" + citationEscape(item.title) + "},\n"
                    + "  author = {" + citationEscape(item.authors).replace(";", " and") + "},\n"
                    + "  year = {" + citationEscape(year) + "},\n"
                    + "  publisher = {" + citationEscape(item.publisher) + "},\n"
                    + "  howpublished = {Repository AIS},\n"
                    + "  note = {" + citationEscape(item.oaiIdentifier) + "}\n}";
        }
        if ("ris".equals(type)) {
            StringBuilder ris = new StringBuilder();
            ris.append("TY  - GEN\r\n");
            String[] authors = safe(item.authors).split(";");
            for (int i = 0; i < authors.length; i++) {
                if (clean(authors[i]).length() > 0) ris.append("AU  - ").append(clean(authors[i])).append("\r\n");
            }
            ris.append("TI  - ").append(safe(item.title)).append("\r\n");
            ris.append("PY  - ").append(year).append("\r\n");
            ris.append("PB  - ").append(safe(item.publisher)).append("\r\n");
            ris.append("UR  - ").append(safe(item.dspaceHandle)).append("\r\n");
            ris.append("ER  - \r\n");
            return ris.toString();
        }
        return safe(item.authors) + " (" + year + "). " + safe(item.title) + ". "
                + safe(item.publisher) + ". " + safe(item.oaiIdentifier);
    }

    private Criteria publicCriteria(Session session, Query ignored) {
        return session.createCriteria(RepoItem.class).add(publicVisibilityRestriction());
    }

    private Criteria searchCriteria(Session session, Query q) {
        Criteria criteria = publicCriteria(session, q);
        if (q.collectionId != null) criteria.add(Restrictions.eq("collectionId", q.collectionId));
        if (q.documentType.length() > 0) criteria.add(Restrictions.eq("documentType", q.documentType));
        if (q.accessPolicy.length() > 0) criteria.add(Restrictions.eq("accessPolicy", q.accessPolicy));
        if (q.year != null) {
            Calendar from = Calendar.getInstance();
            from.clear();
            from.set(Calendar.YEAR, q.year.intValue());
            Calendar until = Calendar.getInstance();
            until.clear();
            until.set(Calendar.YEAR, q.year.intValue() + 1);
            criteria.add(Restrictions.ge("issuedAt", from.getTime()));
            criteria.add(Restrictions.lt("issuedAt", until.getTime()));
        }
        if (q.keyword.length() > 0) {
            criteria.add(Restrictions.or(
                    Restrictions.ilike("title", q.keyword, MatchMode.ANYWHERE),
                    Restrictions.or(
                            Restrictions.ilike("authors", q.keyword, MatchMode.ANYWHERE),
                            Restrictions.or(
                                    Restrictions.ilike("abstractText", q.keyword, MatchMode.ANYWHERE),
                                    Restrictions.or(
                                            Restrictions.ilike("subjects", q.keyword, MatchMode.ANYWHERE),
                                            Restrictions.or(
                                                    Restrictions.ilike("oaiIdentifier", q.keyword, MatchMode.ANYWHERE),
                                                    Restrictions.ilike("dspaceHandle", q.keyword, MatchMode.ANYWHERE)))))));
        }
        return criteria;
    }

    private org.hibernate.criterion.Criterion publicVisibilityRestriction() {
        return Restrictions.and(activeRestriction(), Restrictions.and(
                Restrictions.or(Restrictions.isNull("isWithdrawn"), Restrictions.eq("isWithdrawn", Boolean.FALSE)),
                Restrictions.in("syncStatus", PUBLIC_STATUSES)));
    }

    private org.hibernate.criterion.Criterion activeRestriction() {
        return Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE));
    }

    private long count(Criteria criteria) {
        Object value = criteria.setProjection(Projections.rowCount()).uniqueResult();
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private void applySort(Criteria criteria, String sort) {
        if ("oldest".equals(sort)) {
            criteria.addOrder(Order.asc("issuedAt")).addOrder(Order.asc("id"));
        } else if ("title".equals(sort)) {
            criteria.addOrder(Order.asc("title")).addOrder(Order.desc("id"));
        } else {
            criteria.addOrder(Order.desc("issuedAt")).addOrder(Order.desc("id"));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> groupFacet(Session session, Query q, String property) {
        Criteria criteria = searchCriteria(session, q);
        criteria.setProjection(Projections.projectionList()
                .add(Projections.groupProperty(property))
                .add(Projections.rowCount(), "facetCount"));
        criteria.addOrder(Order.desc("facetCount"));
        List<Object[]> rows = criteria.setMaxResults(30).list();
        Map<String, Long> result = new LinkedHashMap<String, Long>();
        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            String key = row[0] == null ? "Lainnya" : String.valueOf(row[0]);
            long value = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
            result.put(key, Long.valueOf(value));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, RepoCollection> loadCollectionMap(Session session, List<RepoItem> items) {
        if (items == null || items.isEmpty()) return Collections.emptyMap();
        List<Long> ids = new ArrayList<Long>();
        for (int i = 0; i < items.size(); i++) {
            Long id = items.get(i).getCollectionId();
            if (id != null && !ids.contains(id)) ids.add(id);
        }
        if (ids.isEmpty()) return Collections.emptyMap();
        List<RepoCollection> rows = session.createCriteria(RepoCollection.class)
                .add(Restrictions.in("id", ids)).list();
        Map<Long, RepoCollection> result = new HashMap<Long, RepoCollection>();
        for (int i = 0; i < rows.size(); i++) result.put(rows.get(i).getId(), rows.get(i));
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Long> collectionCounts(Session session) {
        Criteria criteria = publicCriteria(session, null);
        criteria.setProjection(Projections.projectionList()
                .add(Projections.groupProperty("collectionId"))
                .add(Projections.rowCount()));
        List<Object[]> rows = criteria.list();
        Map<Long, Long> result = new HashMap<Long, Long>();
        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            if (row[0] instanceof Long && row[1] instanceof Number) {
                result.put((Long) row[0], Long.valueOf(((Number) row[1]).longValue()));
            }
        }
        return result;
    }

    private ItemCard toCard(RepoItem entity, RepoCollection collection) {
        ItemCard card = new ItemCard();
        card.id = entity.getId();
        card.oaiIdentifier = safe(entity.getOaiIdentifier());
        card.dspaceHandle = safe(entity.getDspaceHandle());
        card.title = safe(entity.getTitle());
        card.authors = safe(entity.getAuthors());
        card.abstractText = safe(entity.getAbstractText());
        card.subjects = safe(entity.getSubjects());
        card.documentType = safe(entity.getDocumentType());
        card.accessPolicy = safe(entity.getAccessPolicy());
        card.language = safe(entity.getLanguage());
        card.publisher = safe(entity.getPublisher());
        card.issuedAt = entity.getIssuedAt();
        card.year = entity.getIssuedAt() == null ? "" : new SimpleDateFormat("yyyy").format(entity.getIssuedAt());
        card.collectionId = entity.getCollectionId();
        card.collectionName = collection == null ? "" : safe(collection.getNama());
        return card;
    }

    private void copyCard(ItemCard source, ItemCard target) {
        target.id = source.id;
        target.oaiIdentifier = source.oaiIdentifier;
        target.dspaceHandle = source.dspaceHandle;
        target.title = source.title;
        target.authors = source.authors;
        target.abstractText = source.abstractText;
        target.subjects = source.subjects;
        target.documentType = source.documentType;
        target.accessPolicy = source.accessPolicy;
        target.language = source.language;
        target.publisher = source.publisher;
        target.issuedAt = source.issuedAt;
        target.year = source.year;
        target.collectionId = source.collectionId;
        target.collectionName = source.collectionName;
    }

    private BitstreamView toBitstream(RepoBitstream entity) {
        BitstreamView view = new BitstreamView();
        view.id = entity.getId();
        view.namaFile = safe(entity.getNamaFile());
        view.mimeType = safe(entity.getMimeType());
        view.ukuranByte = entity.getUkuranByte();
        view.checksum = safe(entity.getChecksum());
        view.description = safe(entity.getDescription());
        view.accessPolicy = safe(entity.getAccessPolicy());
        view.primaryFile = Boolean.TRUE.equals(entity.getPrimaryFile());
        return view;
    }

    private boolean canDownload(RepoItem item, RepoBitstream bitstream) {
        if (item == null || bitstream == null) return false;
        if (Boolean.FALSE.equals(item.getAktif()) || Boolean.TRUE.equals(item.getIsWithdrawn())) return false;
        if (!isPublicStatus(item.getSyncStatus())) return false;
        if (!"OPEN_ACCESS".equals(normalizeAccess(item.getAccessPolicy()))) return false;
        String filePolicy = normalizeAccess(bitstream.getAccessPolicy());
        return filePolicy.length() == 0 || "OPEN_ACCESS".equals(filePolicy);
    }

    private boolean isPublicStatus(String value) {
        String status = clean(value).toUpperCase();
        for (int i = 0; i < PUBLIC_STATUSES.length; i++) {
            if (PUBLIC_STATUSES[i].equals(status)) return true;
        }
        return false;
    }

    private boolean isPublicMetadataField(String field) {
        if (field == null) return false;
        if (field.startsWith("dc.")) return true;
        return "repository.programStudy".equals(field)
                || "repository.faculty".equals(field)
                || "repository.institution".equals(field)
                || "repository.orcid".equals(field)
                || "repository.ror".equals(field)
                || "repository.funding".equals(field);
    }

    private String normalizeAccess(String value) {
        String access = clean(value).toUpperCase();
        if ("OPEN_ACCESS".equals(access) || "METADATA_ONLY".equals(access)
                || "INSTITUTION_ONLY".equals(access) || "AUTHENTICATED".equals(access)
                || "RESTRICTED".equals(access) || "EMBARGOED".equals(access)) return access;
        return "";
    }

    private String normalizeSort(String value) {
        String sort = clean(value).toLowerCase();
        return "oldest".equals(sort) || "title".equals(sort) ? sort : "newest";
    }

    private String citationEscape(String value) {
        return safe(value).replace("\\", "\\\\").replace("{", "\\{").replace("}", "\\}");
    }

    public static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String limit(String value, int maximum) {
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }
}
