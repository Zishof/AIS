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
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.security.MessageDigest;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.repository.RepoBitstream;
import ais.database.model.repository.RepoCollection;
import ais.database.model.repository.RepoItem;
import ais.database.model.repository.RepoItemMetadata;
import ais.database.model.repository.RepoItemRelation;
import ais.database.model.repository.RepoUsageEvent;
import ais.database.model.repository.RepoUserPreference;
import ais.database.model.repository.RepoAuthorAuthority;
import ais.database.model.repository.RepoItemContributor;
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
    private static final Set<String> STOPWORDS=new HashSet<String>(Arrays.asList("yang","dan","dengan","untuk","dari","pada","dalam","adalah","atau","the","and","with","from","this","that","study","penelitian"));

    public static class Query {
        public String keyword = "";
        public String searchField = "all";
        public String author = "";
        public String subject = "";
        public String language = "";
        public String identifier = "";
        public String programStudy = "";
        /** metadata = metadata bibliografis saja; all = metadata dan teks hasil ekstraksi. */
        public String searchScope = "all";
        /** WITH_FILE atau METADATA_ONLY. Kosong berarti keduanya. */
        public String fullText = "";
        public String exactPhrase = "";
        public String anyWords = "";
        public String withoutWords = "";
        public boolean semantic;
        public Long collectionId;
        public String documentType = "";
        public String accessPolicy = "";
        public Integer year;
        public Integer yearFrom;
        public Integer yearUntil;
        public Date modifiedFrom;
        public Date modifiedUntil;
        public String sort = "newest";
        public int page = 1;
        public int pageSize = DEFAULT_PAGE_SIZE;
    }

    public static class Summary {
        public long totalItems;
        public long totalCollections;
        public long openAccess;
        public long metadataOnly;
        public long restrictedItems;
        public long embargoedItems;
        public long authorCount;
        public long subjectCount;
        public long documentTypeCount;
        public long openFileItems;
        public long downloads30Days;
        public int metadataQuality;
        public String firstYear = "";
        public String lastYear = "";
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
        public String doi;
        public String licenseUri;
        public Date embargoUntil;
        public long viewCount;
        public long downloadCount;
        public boolean withdrawn;
        public String withdrawalReason;
        public Date withdrawnAt;
        public Date issuedAt;
        public Date datestamp;
        public String year;
        public Long collectionId;
        public String collectionName;
        public long versionNumber;
        public String programStudy = "";
        public int publicFileCount;
        public boolean pdfAvailable;
        public boolean superseded;
        public String authorOrcids = "";
    }

    public static class Suggestion {
        public String type = "item";
        public String label = "";
        public String detail = "";
        public String value = "";
    }

    public static class PreferenceView {
        public Long id,itemId; public String type="",label="",queryValue=""; public Date createdAt;
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
        public List<ItemCard> relatedItems = new ArrayList<ItemCard>();
        public List<ItemCard> versions = new ArrayList<ItemCard>();
    }

    public static class AuthorProfile {
        public String name = "";
        public String orcid = "";
        public Long authorityId;
        public String affiliation = "";
        public String rorId = "";
        public String nameVariants = "";
        public boolean verified;
        public long workCount;
        public List<ItemCard> works = new ArrayList<ItemCard>();
        public Map<String, Long> yearTrend = new LinkedHashMap<String, Long>();
        public Map<String, Long> subjects = new LinkedHashMap<String, Long>();
    }

    public static class SearchResult {
        public Query query;
        public long total;
        public int totalPages;
        public List<ItemCard> items = new ArrayList<ItemCard>();
        public Map<String, Long> typeFacets = new LinkedHashMap<String, Long>();
        public Map<String, Long> accessFacets = new LinkedHashMap<String, Long>();
        public Map<String, Long> yearFacets = new LinkedHashMap<String, Long>();
        public Map<String, Long> authorFacets = new LinkedHashMap<String, Long>();
        public Map<String, Long> subjectFacets = new LinkedHashMap<String, Long>();
        public Map<String, Long> languageFacets = new LinkedHashMap<String, Long>();
        public Map<String, Long> programFacets = new LinkedHashMap<String, Long>();
        public Map<String, Long> sourceFacets = new LinkedHashMap<String, Long>();
        public Map<String, Long> licenseFacets = new LinkedHashMap<String, Long>();
        public Map<String, Long> fullTextFacets = new LinkedHashMap<String, Long>();
        public List<CollectionView> collections = new ArrayList<CollectionView>();
        public String didYouMean = "";
        public boolean synonymExpanded;
    }
    public static class RepositoryAnswer { public String question="",answer="";public List<ItemCard> sources=new ArrayList<ItemCard>(); }
    public static class MetadataSuggestion { public String language="id",documentType="Other",abstractDraft="";public List<String> keywords=new ArrayList<String>();public List<ItemCard> possibleDuplicates=new ArrayList<ItemCard>(); }

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
        q.searchField = normalizeSearchField(q.searchField);
        q.author = limit(clean(q.author), 200);
        q.subject = limit(clean(q.subject), 200);
        q.language = limit(clean(q.language).toLowerCase(), 20);
        q.identifier = limit(clean(q.identifier), 255);
        q.programStudy = limit(clean(q.programStudy), 200);
        q.searchScope = "metadata".equalsIgnoreCase(clean(q.searchScope)) ? "metadata" : "all";
        q.fullText = normalizeFullText(q.fullText);
        q.exactPhrase = limit(clean(q.exactPhrase), 200);
        q.anyWords = limit(clean(q.anyWords), 200);
        q.withoutWords = limit(clean(q.withoutWords), 200);
        q.documentType = limit(clean(q.documentType), 80);
        q.accessPolicy = normalizeAccess(clean(q.accessPolicy));
        q.sort = normalizeSort(q.sort);
        if (q.page < 1) q.page = 1;
        if (q.pageSize < 1) q.pageSize = DEFAULT_PAGE_SIZE;
        if (q.pageSize > MAX_PAGE_SIZE) q.pageSize = MAX_PAGE_SIZE;
        if (q.year != null && (q.year.intValue() < 1000 || q.year.intValue() > 3000)) q.year = null;
        if (q.yearFrom != null && (q.yearFrom.intValue() < 1000 || q.yearFrom.intValue() > 3000)) q.yearFrom = null;
        if (q.yearUntil != null && (q.yearUntil.intValue() < 1000 || q.yearUntil.intValue() > 3000)) q.yearUntil = null;
        if (q.yearFrom != null && q.yearUntil != null && q.yearFrom.intValue() > q.yearUntil.intValue()) {
            Integer swap = q.yearFrom; q.yearFrom = q.yearUntil; q.yearUntil = swap;
        }
        if (q.collectionId != null && q.collectionId.longValue() <= 0L) q.collectionId = null;
        return q;
    }

    public Summary loadSummary() {
        Session session = session();
        Summary summary = new Summary();
        summary.totalItems = count(publicCriteria(session, null));
        summary.totalCollections = count(session.createCriteria(RepoCollection.class)
                .add(activeRestriction()).add(tenantRestriction()));
        summary.openAccess = count(publicCriteria(session, null)
                .add(Restrictions.eq("accessPolicy", "OPEN_ACCESS")));
        summary.metadataOnly = count(publicCriteria(session, null)
                .add(Restrictions.eq("accessPolicy", "METADATA_ONLY")));
        summary.restrictedItems = count(publicCriteria(session, null)
                .add(Restrictions.in("accessPolicy", new String[] { "RESTRICTED", "INSTITUTION_ONLY", "AUTHENTICATED" })));
        summary.embargoedItems = count(publicCriteria(session, null)
                .add(Restrictions.eq("accessPolicy", "EMBARGOED")));
        summary.authorCount = distinctTokenCount(session, "authors");
        summary.subjectCount = distinctTokenCount(session, "subjects");
        Object typeCount = publicCriteria(session, null).setProjection(Projections.countDistinct("documentType")).uniqueResult();
        summary.documentTypeCount = typeCount instanceof Number ? ((Number) typeCount).longValue() : 0L;
        summary.openFileItems = openFileItemCount(session);
        Object recentDownloads=session.createSQLQuery("select count(*) from repo_usage_event e join repo_item i on i.id=e.item_id where e.event_type='DOWNLOAD' and e.occurred_at>=current_timestamp-interval '30 days' and i.tenant_key=:tenant").setString("tenant",RepositoryTenantScope.currentKey()).uniqueResult();
        summary.downloads30Days=recentDownloads instanceof Number?((Number)recentDownloads).longValue():0L;
        summary.metadataQuality = metadataQuality(session);
        Date first = (Date) publicCriteria(session, null).setProjection(Projections.min("issuedAt")).uniqueResult();
        Date last = (Date) publicCriteria(session, null).setProjection(Projections.max("issuedAt")).uniqueResult();
        SimpleDateFormat year = new SimpleDateFormat("yyyy");
        summary.firstYear = first == null ? "" : year.format(first);
        summary.lastYear = last == null ? "" : year.format(last);
        return summary;
    }

    @SuppressWarnings("unchecked")
    public SearchResult search(Query input) {
        Query q = normalize(input);
        Session session = session();
        SearchResult result = new SearchResult();
        result.query = q;
        if(q.semantic&&q.keyword.length()>0){result.items=semanticSearch(q.keyword,q.pageSize);result.total=result.items.size();result.totalPages=result.items.isEmpty()?0:1;result.collections=listCollections(100);return result;}
        result.total = count(searchCriteria(session, q));
        result.synonymExpanded = synonymTerms(q.keyword).size() > 1;
        if (result.total == 0L && q.keyword.length() >= 3) result.didYouMean = suggestCorrection(session, q.keyword);
        result.totalPages = result.total == 0L ? 0 : (int) ((result.total + q.pageSize - 1L) / q.pageSize);
        if (result.totalPages > 0 && q.page > result.totalPages) q.page = result.totalPages;

        Criteria rows = searchCriteria(session, q);
        applySort(rows, q.sort);
        rows.setFirstResult((q.page - 1) * q.pageSize);
        rows.setMaxResults(q.pageSize);
        List<RepoItem> entities = rows.list();

        result.items = cards(session, entities);
        result.typeFacets = groupFacet(session, q, "documentType");
        result.accessFacets = groupFacet(session, q, "accessPolicy");
        result.yearFacets = yearFacet(session, q);
        result.authorFacets = tokenFacet(session,q,"authors"); result.subjectFacets=tokenFacet(session,q,"subjects");
        result.languageFacets=groupFacet(session,q,"language"); result.sourceFacets=groupFacet(session,q,"sourceClass");
        result.licenseFacets=groupFacet(session,q,"licenseUri"); result.programFacets=programFacet(session);
        result.fullTextFacets=fullTextFacet(session,q);
        result.collections = listCollections(100);
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<ItemCard> semanticSearch(String text,int maximum){String query=clean(text).toLowerCase();if(query.length()==0)return new ArrayList<ItemCard>();List<RepoItem> rows=publicCriteria(session(),null).setMaxResults(2500).list();final Map<Long,Integer> scores=new HashMap<Long,Integer>();final List<String> terms=new ArrayList<String>();for(String expanded:synonymTerms(query))for(String token:expanded.split("[^\\p{L}\\p{N}]+"))if(token.length()>2&&!terms.contains(token))terms.add(token);
        for(RepoItem row:rows){String title=safe(row.getTitle()).toLowerCase(),subject=safe(row.getSubjects()).toLowerCase(),body=(safe(row.getAbstractText())+" "+safe(row.getExtractedText())).toLowerCase();int score=0;for(String term:terms){if(title.indexOf(term)>=0)score+=8;if(subject.indexOf(term)>=0)score+=5;if(body.indexOf(term)>=0)score+=2;}if(score>0)scores.put(row.getId(),Integer.valueOf(score));}
        Collections.sort(rows,new java.util.Comparator<RepoItem>(){public int compare(RepoItem a,RepoItem b){return Integer.valueOf(scores.containsKey(b.getId())?scores.get(b.getId()).intValue():0).compareTo(Integer.valueOf(scores.containsKey(a.getId())?scores.get(a.getId()).intValue():0));}});List<RepoItem> selected=new ArrayList<RepoItem>();for(RepoItem row:rows)if(scores.containsKey(row.getId())&&selected.size()<Math.max(1,Math.min(maximum,50)))selected.add(row);return cards(session(),selected);}

    public RepositoryAnswer askRepository(String question){RepositoryAnswer answer=new RepositoryAnswer();answer.question=limit(clean(question),500);answer.sources=semanticSearch(answer.question,5);if(answer.sources.isEmpty()){answer.answer="Belum ditemukan karya repository yang cukup relevan. Coba gunakan istilah yang lebih spesifik.";return answer;}StringBuilder text=new StringBuilder("Berdasarkan karya yang tersedia di repository, topik ini dibahas dalam ");for(int i=0;i<answer.sources.size();i++){ItemCard source=answer.sources.get(i);if(i>0)text.append(i==answer.sources.size()-1?" dan ":", ");text.append('“').append(source.title).append('”');}text.append(". Buka sumber di bawah untuk membaca abstrak dan dokumen sesuai kebijakan akses. Jawaban ini hanya merangkum metadata repository dan tidak menggantikan pembacaan sumber.");answer.answer=text.toString();return answer;}

    public MetadataSuggestion suggestMetadata(String title,String abstractText){MetadataSuggestion suggestion=new MetadataSuggestion();String text=(clean(title)+" "+clean(abstractText)).toLowerCase();suggestion.language=text.matches(".*\\b(the|and|with|from|study)\\b.*")?"en":"id";suggestion.documentType=text.contains("dataset")?"Dataset":(text.contains("skripsi")||text.contains("thesis")?"Thesis":"Article");Map<String,Integer> frequency=new LinkedHashMap<String,Integer>();for(String token:text.split("[^\\p{L}\\p{N}]+"))if(token.length()>4&&!STOPWORDS.contains(token)){Integer n=frequency.get(token);frequency.put(token,Integer.valueOf(n==null?1:n.intValue()+1));}List<Map.Entry<String,Integer>> words=new ArrayList<Map.Entry<String,Integer>>(frequency.entrySet());Collections.sort(words,new java.util.Comparator<Map.Entry<String,Integer>>(){public int compare(Map.Entry<String,Integer>a,Map.Entry<String,Integer>b){return b.getValue().compareTo(a.getValue());}});for(int i=0;i<words.size()&&i<8;i++)suggestion.keywords.add(words.get(i).getKey());suggestion.abstractDraft=clean(abstractText);Query q=new Query();q.keyword=clean(title);q.searchField="title";q.pageSize=5;suggestion.possibleDuplicates=search(q).items;return suggestion;}

    public List<ItemCard> latest(int maximum) {
        Session session = session();
        @SuppressWarnings("unchecked")
        List<RepoItem> rows = publicCriteria(session, null)
                .addOrder(Order.desc("issuedAt")).addOrder(Order.desc("id"))
                .setMaxResults(maximum < 1 ? 6 : Math.min(maximum, MAX_PAGE_SIZE)).list();
        return cards(session, rows);
    }

    @SuppressWarnings("unchecked")
    public List<ItemCard> featured(int maximum){List<RepoItem> rows=publicCriteria(session(),null).add(Restrictions.eq("featured",Boolean.TRUE))
            .addOrder(Order.desc("featuredAt")).addOrder(Order.desc("issuedAt")).setMaxResults(Math.max(1,Math.min(maximum,12))).list();return cards(session(),rows);}

    @SuppressWarnings("unchecked")
    public List<ItemCard> mostDownloaded(String period,int maximum){String window="";if("30d".equals(period))window=" and e.occurred_at>=current_timestamp-interval '30 days'";else if("year".equals(period))window=" and e.occurred_at>=date_trunc('year',current_timestamp)";
        List<Object[]> ranked=session().createSQLQuery("select e.item_id,count(*) hits from repo_usage_event e join repo_item i on i.id=e.item_id where e.event_type='DOWNLOAD' and e.user_agent_class<>'BOT' and i.tenant_key=:tenant"+window+" group by e.item_id order by hits desc limit "+Math.max(1,Math.min(maximum,20)))
                .setString("tenant",RepositoryTenantScope.currentKey()).list();List<Long> ids=new ArrayList<Long>();for(Object[] row:ranked)if(row[0] instanceof Number)ids.add(Long.valueOf(((Number)row[0]).longValue()));if(ids.isEmpty())return new ArrayList<ItemCard>();
        List<RepoItem> items=publicCriteria(session(),null).add(Restrictions.in("id",ids)).list();Map<Long,RepoItem> byId=new HashMap<Long,RepoItem>();for(RepoItem item:items)byId.put(item.getId(),item);List<RepoItem> ordered=new ArrayList<RepoItem>();for(Long id:ids)if(byId.get(id)!=null)ordered.add(byId.get(id));return cards(session(),ordered);}

    public List<CollectionView> popularCollections(int maximum) {
        List<CollectionView> rows = listCollections(500);
        Collections.sort(rows, new java.util.Comparator<CollectionView>() {
            public int compare(CollectionView a, CollectionView b) {
                int count = Long.valueOf(b.itemCount).compareTo(Long.valueOf(a.itemCount));
                return count != 0 ? count : safe(a.nama).compareToIgnoreCase(safe(b.nama));
            }
        });
        List<CollectionView> result = new ArrayList<CollectionView>();
        int limit = maximum < 1 ? 6 : Math.min(maximum, 20);
        for (CollectionView row : rows) {
            if (row.itemCount > 0L && result.size() < limit) result.add(row);
        }
        return result;
    }

    public Map<String, Long> popularSubjects(int maximum) {
        return top(tokenFacet(session(), new Query(), "subjects"), maximum < 1 ? 8 : Math.min(maximum, 30));
    }

    public boolean hasDepositCollection() {
        return count(session().createCriteria(RepoCollection.class).add(activeRestriction())
                .add(tenantRestriction()).add(Restrictions.or(Restrictions.isNull("depositEnabled"), Restrictions.eq("depositEnabled", Boolean.TRUE)))) > 0L;
    }

    @SuppressWarnings("unchecked")
    public List<PreferenceView> preferences(String userId, String type, int maximum) {
        String owner=limit(clean(userId),255), kind=normalizePreferenceType(type);
        List<PreferenceView> result=new ArrayList<PreferenceView>(); if(owner.length()==0||kind.length()==0)return result;
        List<RepoUserPreference> rows=session().createCriteria(RepoUserPreference.class)
                .add(Restrictions.eq("tenantKey",RepositoryTenantScope.currentKey())).add(Restrictions.eq("userId",owner)).add(Restrictions.eq("preferenceType",kind)).add(activeRestriction())
                .addOrder(Order.desc("createdAt")).setMaxResults(maximum<1?20:Math.min(maximum,100)).list();
        for(RepoUserPreference row:rows){PreferenceView v=new PreferenceView();v.id=row.getId();v.itemId=row.getItemId();v.type=kind;v.label=safe(row.getLabel());v.queryValue=safe(row.getQueryValue());v.createdAt=row.getCreatedAt();result.add(v);}return result;
    }

    public boolean toggleBookmark(String userId, Long itemId) {
        String owner=limit(clean(userId),255);if(owner.length()==0||itemId==null||findPublicItem(itemId)==null)throw new SecurityException("Bookmark memerlukan pengguna dan item publik yang valid.");
        Session s=session();org.hibernate.Transaction tx=s.beginTransaction();try{
            RepoUserPreference row=(RepoUserPreference)s.createCriteria(RepoUserPreference.class).add(Restrictions.eq("userId",owner))
                    .add(Restrictions.eq("tenantKey",RepositoryTenantScope.currentKey())).add(Restrictions.eq("preferenceType","BOOKMARK")).add(Restrictions.eq("itemId",itemId)).setMaxResults(1).uniqueResult();
            boolean active=row==null||!Boolean.TRUE.equals(row.getAktif());if(row==null){row=new RepoUserPreference();row.setTenantKey(RepositoryTenantScope.currentKey());row.setUserId(owner);row.setPreferenceType("BOOKMARK");row.setItemId(itemId);row.setLabel("");row.setQueryValue("");row.setCreatedAt(new Date());}
            row.setAktif(Boolean.valueOf(active));s.saveOrUpdate(row);tx.commit();return active;
        }catch(RuntimeException e){if(tx.isActive())tx.rollback();throw e;}
    }

    public void saveSearch(String userId,String label,String queryValue,boolean alert) {
        String owner=limit(clean(userId),255),query=limit(clean(queryValue),2000);if(owner.length()==0||query.length()==0)throw new IllegalArgumentException("Pencarian yang disimpan tidak valid.");
        Session s=session();org.hibernate.Transaction tx=s.beginTransaction();try{
            RepoUserPreference row=(RepoUserPreference)s.createCriteria(RepoUserPreference.class).add(Restrictions.eq("userId",owner))
                    .add(Restrictions.eq("tenantKey",RepositoryTenantScope.currentKey())).add(Restrictions.eq("preferenceType",alert?"SEARCH_ALERT":"SAVED_SEARCH")).add(Restrictions.eq("queryValue",query)).setMaxResults(1).uniqueResult();
            if(row==null){row=new RepoUserPreference();row.setTenantKey(RepositoryTenantScope.currentKey());row.setUserId(owner);row.setPreferenceType(alert?"SEARCH_ALERT":"SAVED_SEARCH");row.setQueryValue(query);row.setCreatedAt(new Date());}
            row.setLabel(limit(clean(label).length()==0?"Pencarian repository":clean(label),255));row.setAktif(Boolean.TRUE);s.saveOrUpdate(row);tx.commit();
        }catch(RuntimeException e){if(tx.isActive())tx.rollback();throw e;}
    }

    public void removePreference(String userId,Long id) {
        String owner=limit(clean(userId),255);if(owner.length()==0||id==null)return;Session s=session();org.hibernate.Transaction tx=s.beginTransaction();try{
            RepoUserPreference row=(RepoUserPreference)s.createCriteria(RepoUserPreference.class).add(Restrictions.eq("id",id)).add(Restrictions.eq("tenantKey",RepositoryTenantScope.currentKey())).add(Restrictions.eq("userId",owner)).uniqueResult();
            if(row!=null){row.setAktif(Boolean.FALSE);s.update(row);}tx.commit();
        }catch(RuntimeException e){if(tx.isActive())tx.rollback();throw e;}
    }

    @SuppressWarnings("unchecked")
    public List<ItemCard> recommendations(String userId,int maximum) {
        List<PreferenceView> bookmarks=preferences(userId,"BOOKMARK",30);if(bookmarks.isEmpty())return new ArrayList<ItemCard>();
        List<Long> ids=new ArrayList<Long>();for(PreferenceView v:bookmarks)if(v.itemId!=null)ids.add(v.itemId);if(ids.isEmpty())return new ArrayList<ItemCard>();
        List<RepoItem> source=publicCriteria(session(),null).add(Restrictions.in("id",ids)).list();org.hibernate.criterion.Disjunction topics=Restrictions.disjunction();boolean hasTopic=false;
        for(RepoItem row:source){String token=firstToken(row.getSubjects());if(token.length()>0){topics.add(Restrictions.ilike("subjects",token,MatchMode.ANYWHERE));hasTopic=true;}}
        if(!hasTopic)return new ArrayList<ItemCard>();
        List<RepoItem> rows=publicCriteria(session(),null).add(topics).add(Restrictions.not(Restrictions.in("id",ids))).addOrder(Order.desc("issuedAt"))
                .setMaxResults(maximum<1?4:Math.min(maximum,12)).list();return cards(session(),rows);
    }

    @SuppressWarnings("unchecked")
    public List<Suggestion> suggest(String text, String field, int maximum) {
        Query q = new Query();
        q.keyword = text;
        q.searchField = field;
        normalize(q);
        List<Suggestion> result = new ArrayList<Suggestion>();
        if (q.keyword.length() < 2) return result;
        int limit = maximum < 1 ? 8 : Math.min(maximum, 12);
        List<RepoItem> rows = searchCriteria(session(), q).addOrder(Order.asc("title"))
                .setMaxResults(Math.max(limit * 3, 12)).list();
        Map<String, Boolean> seen = new LinkedHashMap<String, Boolean>();
        for (RepoItem row : rows) {
            String[] candidates;
            String[] types;
            if ("author".equals(q.searchField)) { candidates = safe(row.getAuthors()).split("[;,]"); types = repeatedType(candidates.length, "author"); }
            else if ("subject".equals(q.searchField)) { candidates = safe(row.getSubjects()).split("[;,]"); types = repeatedType(candidates.length, "subject"); }
            else if ("identifier".equals(q.searchField)) { candidates = new String[] { safe(row.getDoi()), safe(row.getOaiIdentifier()), safe(row.getDspaceHandle()) }; types = repeatedType(candidates.length, "identifier"); }
            else if ("all".equals(q.searchField)) {
                String firstAuthor = firstToken(row.getAuthors()); String firstSubject = firstToken(row.getSubjects());
                candidates = new String[] { safe(row.getTitle()), firstAuthor, firstSubject, safe(row.getDoi()), safe(row.getOaiIdentifier()) };
                types = new String[] { "title", "author", "subject", "identifier", "identifier" };
            } else { candidates = new String[] { safe(row.getTitle()) }; types = repeatedType(1, q.searchField); }
            for (int candidateIndex = 0; candidateIndex < candidates.length; candidateIndex++) {
                String candidate = candidates[candidateIndex]; String type = types[candidateIndex];
                String value = clean(candidate);
                if (value.length() == 0 || value.toLowerCase().indexOf(q.keyword.toLowerCase()) < 0 || seen.containsKey(value.toLowerCase())) continue;
                Suggestion suggestion = new Suggestion(); suggestion.type = type; suggestion.label = value;
                suggestion.value = value; suggestion.detail = "item".equals(type) ? safe(row.getAuthors()) : safe(row.getTitle());
                result.add(suggestion); seen.put(value.toLowerCase(), Boolean.TRUE);
                if (result.size() >= limit) return result;
            }
        }
        return result;
    }

    private String[] repeatedType(int size, String type) {
        String[] result = new String[Math.max(0, size)];
        for (int i = 0; i < result.length; i++) result[i] = type;
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<CollectionView> listCollections(int maximum) {
        Session session = session();
        List<RepoCollection> rows = session.createCriteria(RepoCollection.class)
                .add(activeRestriction())
                .add(tenantRestriction())
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
                .add(tenantRestriction())
                .uniqueResult();
        if (entity == null) return null;

        RepoCollection collection = (RepoCollection) session.get(RepoCollection.class, entity.getCollectionId());
        if(collection!=null&&!RepositoryTenantScope.currentKey().equals(collection.getTenantKey()))collection=null;
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
            if ("repository.programStudy".equals(field) && detail.programStudy.length() == 0) detail.programStudy = safe(metadata.getMetadataValue());
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
            detail.publicFileCount++;
            String mime = safe(bitstream.getMimeType()).toLowerCase(); String name = safe(bitstream.getNamaFile()).toLowerCase();
            if (mime.indexOf("pdf") >= 0 || name.endsWith(".pdf")) detail.pdfAvailable = true;
        }
        detail.relatedItems = relatedItems(session, entity, 8);
        detail.versions = versionItems(session, entity, 20);
        return detail;
    }

    /**
     * Lookup ringan untuk ekspor sitasi. Sitasi hanya membutuhkan metadata inti
     * RepoItem, sehingga tidak perlu memuat bitstream, metadata tambahan,
     * relasi, dan seluruh riwayat versi seperti halaman detail publikasi.
     */
    public ItemDetail findPublicCitationItem(Long id) {
        if (id == null || id.longValue() <= 0L) return null;
        Session session = session();
        RepoItem entity = (RepoItem) session.createCriteria(RepoItem.class)
                .add(Restrictions.eq("id", id))
                .add(publicVisibilityRestriction())
                .add(tenantRestriction())
                .uniqueResult();
        if (entity == null) return null;
        RepoCollection collection = (RepoCollection) session.get(RepoCollection.class, entity.getCollectionId());
        if (collection != null && !RepositoryTenantScope.currentKey().equals(collection.getTenantKey())) collection = null;
        ItemDetail detail = new ItemDetail();
        copyCard(toCard(entity, collection), detail);
        return detail;
    }

    public CollectionView findCollection(Long id) {
        if (id == null) return null;
        RepoCollection row = (RepoCollection) session().createCriteria(RepoCollection.class)
                .add(Restrictions.eq("id", id)).add(activeRestriction()).add(tenantRestriction()).uniqueResult();
        if (row == null) return null;
        CollectionView view = new CollectionView(); view.id=row.getId(); view.kode=safe(row.getKode());
        view.nama=safe(row.getNama()); view.deskripsi=safe(row.getDeskripsi()); view.tipe=safe(row.getTipe());
        Long count=collectionCounts(session()).get(row.getId()); view.itemCount=count==null?0L:count.longValue(); return view;
    }

    public AuthorProfile authorProfile(String name) {
        String author = limit(clean(name), 200); if (author.length() == 0) return null;
        Query q = new Query(); q.author=author; q.sort="newest"; q.pageSize=MAX_PAGE_SIZE;
        SearchResult result=search(q); if(result.total==0)return null;
        AuthorProfile profile=new AuthorProfile(); profile.name=author; profile.workCount=result.total; profile.works=result.items;
        RepoAuthorAuthority authority=(RepoAuthorAuthority)session().createCriteria(RepoAuthorAuthority.class)
                .add(Restrictions.eq("tenantKey",RepositoryTenantScope.currentKey()))
                .add(Restrictions.eq("normalizedName",RepositoryAuthorityService.normalizeName(author)))
                .add(activeRestriction()).setMaxResults(1).uniqueResult();
        if(authority!=null){profile.authorityId=authority.getId();profile.name=authority.getCanonicalName();profile.orcid=authority.getOrcid();
            profile.affiliation=authority.getAffiliation();profile.rorId=authority.getRorId();profile.nameVariants=authority.getNameVariants();profile.verified=Boolean.TRUE.equals(authority.getVerified());}
        for(ItemCard item:result.items){if(item.year.length()>0)increment(profile.yearTrend,item.year);for(String s:safe(item.subjects).split("[;,]"))if(clean(s).length()>0)increment(profile.subjects,clean(s));}
        return profile;
    }

    public ItemDetail findPublicItemByOai(String identifier) {
        String value = limit(clean(identifier), 255);
        if (value.length() == 0) return null;
        RepoItem item = (RepoItem) session().createCriteria(RepoItem.class)
                .add(Restrictions.eq("oaiIdentifier", value))
                .add(publicVisibilityRestriction())
                .add(tenantRestriction())
                .uniqueResult();
        return item == null ? null : findPublicItem(item.getId());
    }

    /** Record OAI dapat berupa record publik atau tombstone yang telah ditarik. */
    public ItemDetail findOaiItemByIdentifier(String identifier) {
        String value = limit(clean(identifier), 255);
        if (value.length() == 0) return null;
        RepoItem item = (RepoItem) session().createCriteria(RepoItem.class)
                .add(Restrictions.eq("oaiIdentifier", value)).add(activeRestriction()).add(tenantRestriction())
                .uniqueResult();
        if (item == null) return null;
        ItemDetail published = findPublicItem(item.getId());
        return published != null ? published : findTombstone(item.getId());
    }

    public ItemDetail findTombstone(Long id) {
        if(id==null)return null;Session session=session();RepoItem entity=(RepoItem)session.createCriteria(RepoItem.class)
                .add(Restrictions.eq("id",id)).add(activeRestriction()).add(tenantRestriction()).add(Restrictions.eq("isWithdrawn",Boolean.TRUE)).uniqueResult();
        if(entity==null)return null;RepoCollection c=(RepoCollection)session.get(RepoCollection.class,entity.getCollectionId());if(c!=null&&!RepositoryTenantScope.currentKey().equals(c.getTenantKey()))c=null;ItemDetail d=new ItemDetail();copyCard(toCard(entity,c),d);d.authors="";d.abstractText="";d.subjects="";return d;
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
                .add(tenantRestriction())
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
        if ("endnote".equals(type)) {
            StringBuilder e = new StringBuilder(); e.append("%0 Generic\r\n");
            String[] authors = safe(item.authors).split(";"); for(String author:authors)if(clean(author).length()>0)e.append("%A ").append(clean(author)).append("\r\n");
            e.append("%T ").append(safe(item.title)).append("\r\n%8 ").append(year).append("\r\n%I ").append(safe(item.publisher)).append("\r\n");
            if(clean(item.doi).length()>0)e.append("%R ").append(item.doi).append("\r\n"); e.append("%U ").append(safe(item.dspaceHandle)).append("\r\n"); return e.toString();
        }
        if ("csl".equals(type)) {
            try { JSONObject c=new JSONObject();c.put("id","ais-repository-"+item.id);c.put("type","article");c.put("title",item.title);c.put("issued",new JSONObject().put("raw",year));c.put("publisher",item.publisher);c.put("DOI",item.doi);c.put("URL",item.dspaceHandle);JSONArray a=new JSONArray();for(String author:safe(item.authors).split(";"))if(clean(author).length()>0)a.put(new JSONObject().put("literal",clean(author)));c.put("author",a);return c.toString(2);}catch(Exception e){throw new IllegalStateException(e);}
        }
        if ("dcxml".equals(type)) {
            StringBuilder x=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">");
            x.append("<dc:title>").append(xmlEscape(item.title)).append("</dc:title>");for(String author:safe(item.authors).split(";"))if(clean(author).length()>0)x.append("<dc:creator>").append(xmlEscape(clean(author))).append("</dc:creator>");
            x.append("<dc:date>").append(xmlEscape(year)).append("</dc:date><dc:type>").append(xmlEscape(item.documentType)).append("</dc:type><dc:identifier>").append(xmlEscape(item.oaiIdentifier)).append("</dc:identifier></oai_dc:dc>");return x.toString();
        }
        if ("apa".equals(type)) return safe(item.authors)+" ("+year+"). "+safe(item.title)+". "+safe(item.publisher)+". "+safe(item.doi);
        if ("ieee".equals(type)) return safe(item.authors)+", “"+safe(item.title)+",” "+safe(item.publisher)+", "+year+". "+safe(item.doi);
        if ("harvard".equals(type)) return safe(item.authors)+" ("+year+") ‘"+safe(item.title)+"’, "+safe(item.publisher)+". "+citationIdentifier(item);
        if ("vancouver".equals(type)) return safe(item.authors)+". "+safe(item.title)+". "+safe(item.publisher)+"; "+year+". "+citationIdentifier(item);
        if ("chicago".equals(type)) return safe(item.authors)+". “"+safe(item.title)+".” "+safe(item.publisher)+", "+year+". "+safe(item.doi);
        return safe(item.authors) + " (" + year + "). " + safe(item.title) + ". "
                + safe(item.publisher) + ". " + safe(item.oaiIdentifier);
    }

    private Criteria publicCriteria(Session session, Query ignored) {
        return session.createCriteria(RepoItem.class).add(publicVisibilityRestriction()).add(tenantRestriction());
    }

    private Criteria searchCriteria(Session session, Query q) {
        Criteria criteria = publicCriteria(session, q);
        if (q.collectionId != null) criteria.add(Restrictions.eq("collectionId", q.collectionId));
        if (q.documentType.length() > 0) criteria.add(Restrictions.eq("documentType", q.documentType));
        if (q.accessPolicy.length() > 0) criteria.add(Restrictions.eq("accessPolicy", q.accessPolicy));
        if (q.author.length() > 0) criteria.add(Restrictions.or(
                Restrictions.ilike("authors", q.author, MatchMode.ANYWHERE),
                Restrictions.ilike("authors", reversedName(q.author), MatchMode.ANYWHERE)));
        if (q.subject.length() > 0) criteria.add(Restrictions.ilike("subjects", q.subject, MatchMode.ANYWHERE));
        if (q.language.length() > 0) criteria.add(Restrictions.eq("language", q.language));
        if (q.identifier.length() > 0) criteria.add(Restrictions.or(
                Restrictions.ilike("oaiIdentifier", q.identifier, MatchMode.ANYWHERE),
                Restrictions.ilike("dspaceHandle", q.identifier, MatchMode.ANYWHERE)));
        if(q.programStudy.length()>0)criteria.add(Restrictions.sqlRestriction(
                "exists (select 1 from repo_item_metadata rpm where rpm.item_id={alias}.id and rpm.metadata_field='repository.programStudy' and lower(rpm.metadata_value) like lower(?))",
                "%"+q.programStudy+"%",Hibernate.STRING));
        if ("WITH_FILE".equals(q.fullText)) criteria.add(Restrictions.sqlRestriction(
                "{alias}.access_policy='OPEN_ACCESS' and exists (select 1 from repo_bitstream rb where rb.item_id={alias}.id and coalesce(rb.aktif,true)=true and rb.access_policy='OPEN_ACCESS')"));
        else if ("METADATA_ONLY".equals(q.fullText)) criteria.add(Restrictions.sqlRestriction(
                "not (coalesce({alias}.access_policy,'METADATA_ONLY')='OPEN_ACCESS' and exists (select 1 from repo_bitstream rb where rb.item_id={alias}.id and coalesce(rb.aktif,true)=true and rb.access_policy='OPEN_ACCESS'))"));
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
        if (q.yearFrom != null) {
            Calendar from = Calendar.getInstance(); from.clear(); from.set(Calendar.YEAR, q.yearFrom.intValue());
            criteria.add(Restrictions.ge("issuedAt", from.getTime()));
        }
        if (q.yearUntil != null) {
            Calendar until = Calendar.getInstance(); until.clear(); until.set(Calendar.YEAR, q.yearUntil.intValue() + 1);
            criteria.add(Restrictions.lt("issuedAt", until.getTime()));
        }
        if (q.modifiedFrom != null) criteria.add(Restrictions.sqlRestriction(
                "coalesce({alias}.last_sync_at,{alias}.published_at,{alias}.issued_at,{alias}.submitted_at) >= ?",
                q.modifiedFrom, Hibernate.TIMESTAMP));
        if (q.modifiedUntil != null) criteria.add(Restrictions.sqlRestriction(
                "coalesce({alias}.last_sync_at,{alias}.published_at,{alias}.issued_at,{alias}.submitted_at) <= ?",
                q.modifiedUntil, Hibernate.TIMESTAMP));
        if (q.keyword.length() > 0) {
            if ("title".equals(q.searchField)) criteria.add(Restrictions.ilike("title", q.keyword, MatchMode.ANYWHERE));
            else if ("author".equals(q.searchField)) criteria.add(Restrictions.or(
                    Restrictions.ilike("authors", q.keyword, MatchMode.ANYWHERE),
                    Restrictions.ilike("authors", reversedName(q.keyword), MatchMode.ANYWHERE)));
            else if ("subject".equals(q.searchField)) criteria.add(Restrictions.ilike("subjects", q.keyword, MatchMode.ANYWHERE));
            else if ("abstract".equals(q.searchField)) criteria.add(Restrictions.ilike("abstractText", q.keyword, MatchMode.ANYWHERE));
            else if ("fulltext".equals(q.searchField)) criteria.add(Restrictions.ilike("extractedText", q.keyword, MatchMode.ANYWHERE));
            else if ("program".equals(q.searchField)) criteria.add(Restrictions.sqlRestriction(
                    "exists (select 1 from repo_item_metadata rpm where rpm.item_id={alias}.id and rpm.metadata_field='repository.programStudy' and lower(rpm.metadata_value) like lower(?))",
                    "%" + q.keyword + "%", Hibernate.STRING));
            else if ("advisor".equals(q.searchField)) criteria.add(Restrictions.sqlRestriction(
                    "exists (select 1 from repo_item_metadata rpm where rpm.item_id={alias}.id and rpm.metadata_field in ('dc.contributor.advisor','dc.contributor.editor','repository.advisor') and lower(rpm.metadata_value) like lower(?))",
                    "%" + q.keyword + "%", Hibernate.STRING));
            else if ("identifier".equals(q.searchField)) criteria.add(Restrictions.or(
                    Restrictions.ilike("doi", q.keyword, MatchMode.ANYWHERE), Restrictions.or(
                    Restrictions.ilike("oaiIdentifier", q.keyword, MatchMode.ANYWHERE),
                    Restrictions.ilike("dspaceHandle", q.keyword, MatchMode.ANYWHERE))));
            else {
                String indexedText = "metadata".equals(q.searchScope)
                        ? "coalesce(title,'') || ' ' || coalesce(authors,'') || ' ' || coalesce(subjects,'') || ' ' || coalesce(abstract_text,'')"
                        : "coalesce(title,'') || ' ' || coalesce(authors,'') || ' ' || coalesce(subjects,'') || ' ' || coalesce(abstract_text,'') || ' ' || coalesce(extracted_text,'')";
                org.hibernate.criterion.Disjunction ftsTerms = Restrictions.disjunction();
                for (String term : synonymTerms(q.keyword)) ftsTerms.add(Restrictions.sqlRestriction(
                        "to_tsvector('simple', " + indexedText + ") @@ plainto_tsquery('simple', ?)", term, Hibernate.STRING));
                org.hibernate.criterion.Criterion eav=Restrictions.sqlRestriction(
                        "exists (select 1 from repo_item_metadata rpm where rpm.item_id={alias}.id and coalesce(rpm.aktif,true)=true and lower(rpm.metadata_value) like lower(?))",
                        "%"+q.keyword+"%",Hibernate.STRING);
                criteria.add(Restrictions.or(ftsTerms, Restrictions.or(eav, Restrictions.or(
                        Restrictions.ilike("oaiIdentifier", q.keyword, MatchMode.ANYWHERE),
                        Restrictions.ilike("dspaceHandle", q.keyword, MatchMode.ANYWHERE)))));
            }
        }
        if (q.exactPhrase.length() > 0) criteria.add(Restrictions.or(
                Restrictions.ilike("title", q.exactPhrase, MatchMode.ANYWHERE), Restrictions.or(
                Restrictions.ilike("abstractText", q.exactPhrase, MatchMode.ANYWHERE),
                Restrictions.ilike("extractedText", q.exactPhrase, MatchMode.ANYWHERE))));
        if (q.anyWords.length() > 0) {
            org.hibernate.criterion.Disjunction any = Restrictions.disjunction();
            for (String word : q.anyWords.split("\\s+")) if (clean(word).length() > 1) any.add(Restrictions.or(
                    Restrictions.ilike("title", clean(word), MatchMode.ANYWHERE), Restrictions.or(
                    Restrictions.ilike("subjects", clean(word), MatchMode.ANYWHERE),
                    Restrictions.ilike("abstractText", clean(word), MatchMode.ANYWHERE))));
            criteria.add(any);
        }
        if (q.withoutWords.length() > 0) for (String word : q.withoutWords.split("\\s+")) if (clean(word).length() > 1)
            criteria.add(Restrictions.not(Restrictions.or(Restrictions.ilike("title", clean(word), MatchMode.ANYWHERE),
                    Restrictions.or(Restrictions.ilike("subjects", clean(word), MatchMode.ANYWHERE),
                    Restrictions.ilike("abstractText", clean(word), MatchMode.ANYWHERE)))));
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

    private org.hibernate.criterion.Criterion tenantRestriction(){return Restrictions.eq("tenantKey",RepositoryTenantScope.currentKey());}


    private long count(Criteria criteria) {
        Object value = criteria.setProjection(Projections.rowCount()).uniqueResult();
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private void applySort(Criteria criteria, String sort) {
        if ("oldest".equals(sort)) {
            criteria.addOrder(Order.asc("issuedAt")).addOrder(Order.asc("id"));
        } else if ("title".equals(sort)) {
            criteria.addOrder(Order.asc("title")).addOrder(Order.desc("id"));
        } else if ("author".equals(sort)) {
            criteria.addOrder(Order.asc("authors")).addOrder(Order.asc("title"));
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
    private Map<String, Long> yearFacet(Session session, Query q) {
        Query withoutYear = new Query(); withoutYear.keyword=q.keyword; withoutYear.searchField=q.searchField; withoutYear.author=q.author; withoutYear.subject=q.subject; withoutYear.language=q.language; withoutYear.identifier=q.identifier;withoutYear.programStudy=q.programStudy; withoutYear.collectionId=q.collectionId; withoutYear.documentType=q.documentType; withoutYear.accessPolicy=q.accessPolicy;
        List<RepoItem> rows = searchCriteria(session, withoutYear).addOrder(Order.desc("issuedAt")).setMaxResults(5000).list();
        Map<String,Long> result=new LinkedHashMap<String,Long>(); SimpleDateFormat f=new SimpleDateFormat("yyyy");
        for(RepoItem row:rows){if(row.getIssuedAt()==null)continue;String y=f.format(row.getIssuedAt());Long n=result.get(y);result.put(y,Long.valueOf(n==null?1:n.longValue()+1));} return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String,Long> tokenFacet(Session session,Query q,String property){List<RepoItem> rows=searchCriteria(session,q).setMaxResults(5000).list();Map<String,Long> out=new LinkedHashMap<String,Long>();for(RepoItem row:rows){String value="authors".equals(property)?row.getAuthors():row.getSubjects();for(String token:safe(value).split("[;,]")){String key=clean(token);if(key.length()>0)increment(out,key);}}return top(out,30);}
    @SuppressWarnings("unchecked")
    private Map<String,Long> programFacet(Session session){List<Object[]> rows=session.createSQLQuery("select m.metadata_value,count(*) from repo_item_metadata m join repo_item i on i.id=m.item_id where m.metadata_field='repository.programStudy' and coalesce(m.aktif,true)=true and coalesce(i.aktif,true)=true and coalesce(i.is_withdrawn,false)=false and i.sync_status in ('SYNCED','PUBLISHED','APPROVED') and i.tenant_key=:tenant group by m.metadata_value order by count(*) desc limit 30").setString("tenant",RepositoryTenantScope.currentKey()).list();Map<String,Long> out=new LinkedHashMap<String,Long>();for(Object[] row:rows)if(row[0]!=null&&row[1] instanceof Number)out.put(String.valueOf(row[0]),Long.valueOf(((Number)row[1]).longValue()));return out;}
    @SuppressWarnings("unchecked")
    private Map<String,Long> fullTextFacet(Session session,Query q){List<RepoItem> items=searchCriteria(session,q).setMaxResults(5000).list();List<Long> ids=new ArrayList<Long>();for(RepoItem i:items)if("OPEN_ACCESS".equals(i.getAccessPolicy()))ids.add(i.getId());long with=0;if(!ids.isEmpty()){List<Object> rows=session.createCriteria(RepoBitstream.class).add(activeRestriction()).add(Restrictions.eq("accessPolicy","OPEN_ACCESS")).add(Restrictions.in("itemId",ids)).setProjection(Projections.distinct(Projections.property("itemId"))).list();with=rows.size();}Map<String,Long> out=new LinkedHashMap<String,Long>();out.put("WITH_FILE",Long.valueOf(with));out.put("METADATA_ONLY",Long.valueOf(items.size()-with));return out;}
    private static Map<String,Long> top(Map<String,Long> source,int maximum){List<Map.Entry<String,Long>> rows=new ArrayList<Map.Entry<String,Long>>(source.entrySet());Collections.sort(rows,new java.util.Comparator<Map.Entry<String,Long>>(){public int compare(Map.Entry<String,Long>a,Map.Entry<String,Long>b){return b.getValue().compareTo(a.getValue());}});Map<String,Long> out=new LinkedHashMap<String,Long>();for(int i=0;i<rows.size()&&i<maximum;i++)out.put(rows.get(i).getKey(),rows.get(i).getValue());return out;}

    private long distinctTokenCount(Session session, String property) {
        String column = "subjects".equals(property) ? "subjects" : "authors";
        Object value = session.createSQLQuery("select count(distinct lower(trim(token))) from repo_item i cross join lateral regexp_split_to_table(coalesce(i."
                + column + ",''),'[;,]') as tokens(token) where trim(token)<>'' and coalesce(i.aktif,true)=true and coalesce(i.is_withdrawn,false)=false and i.sync_status in ('SYNCED','PUBLISHED','APPROVED') and i.tenant_key=:tenant").setString("tenant",RepositoryTenantScope.currentKey()).uniqueResult();
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private int metadataQuality(Session session) {
        Object value = session.createSQLQuery("select round(100.0*avg(((case when nullif(trim(title),'') is not null then 1 else 0 end)+(case when nullif(trim(authors),'') is not null then 1 else 0 end)+(case when nullif(trim(abstract_text),'') is not null then 1 else 0 end)+(case when nullif(trim(subjects),'') is not null then 1 else 0 end)+(case when issued_at is not null then 1 else 0 end))/5.0)) from repo_item i where coalesce(i.aktif,true)=true and coalesce(i.is_withdrawn,false)=false and i.sync_status in ('SYNCED','PUBLISHED','APPROVED') and i.tenant_key=:tenant").setString("tenant",RepositoryTenantScope.currentKey()).uniqueResult();
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private long openFileItemCount(Session session) {
        Object value = session.createSQLQuery("select count(distinct b.item_id) from repo_bitstream b join repo_item i on i.id=b.item_id where coalesce(b.aktif,true)=true and b.access_policy='OPEN_ACCESS' and coalesce(i.aktif,true)=true and coalesce(i.is_withdrawn,false)=false and i.sync_status in ('SYNCED','PUBLISHED','APPROVED') and i.access_policy='OPEN_ACCESS' and i.tenant_key=:tenant").setString("tenant",RepositoryTenantScope.currentKey()).uniqueResult();
        return value instanceof Number ? ((Number) value).longValue() : 0L;
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
                .add(Restrictions.in("id", ids)).add(tenantRestriction()).list();
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
        card.doi = safe(entity.getDoi());
        card.licenseUri = safe(entity.getLicenseUri());
        card.embargoUntil = entity.getEmbargoUntil();
        card.datestamp = entity.getLastSyncAt() != null ? entity.getLastSyncAt()
                : (entity.getPublishedAt() != null ? entity.getPublishedAt()
                : (entity.getIssuedAt() != null ? entity.getIssuedAt() : entity.getSubmittedAt()));
        card.viewCount = entity.getViewCount() == null ? 0L : entity.getViewCount().longValue();
        card.downloadCount = entity.getDownloadCount() == null ? 0L : entity.getDownloadCount().longValue();
        card.withdrawn = Boolean.TRUE.equals(entity.getIsWithdrawn()); card.withdrawalReason=safe(entity.getWithdrawalReason()); card.withdrawnAt=entity.getWithdrawnAt();
        card.issuedAt = entity.getIssuedAt();
        card.year = entity.getIssuedAt() == null ? "" : new SimpleDateFormat("yyyy").format(entity.getIssuedAt());
        card.collectionId = entity.getCollectionId();
        card.collectionName = collection == null ? "" : safe(collection.getNama());
        card.versionNumber = entity.getVersionNumber() == null ? 1L : entity.getVersionNumber().longValue();
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
        target.doi = source.doi; target.licenseUri=source.licenseUri; target.embargoUntil=source.embargoUntil; target.datestamp=source.datestamp;
        target.viewCount=source.viewCount; target.downloadCount=source.downloadCount;
        target.withdrawn=source.withdrawn;target.withdrawalReason=source.withdrawalReason;target.withdrawnAt=source.withdrawnAt;
        target.issuedAt = source.issuedAt;
        target.year = source.year;
        target.collectionId = source.collectionId;
        target.collectionName = source.collectionName;
        target.versionNumber = source.versionNumber;
        target.programStudy = source.programStudy;
        target.publicFileCount = source.publicFileCount;
        target.pdfAvailable = source.pdfAvailable;
        target.superseded = source.superseded;
        target.authorOrcids = source.authorOrcids;
    }

    @SuppressWarnings("unchecked")
    private List<ItemCard> relatedItems(Session session, RepoItem item, int maximum) {
        List<RepoItemRelation> relations=session.createCriteria(RepoItemRelation.class).add(activeRestriction())
                .add(Restrictions.or(Restrictions.eq("itemId",item.getId()),Restrictions.eq("relatedItemId",item.getId())))
                .setMaxResults(maximum*2).list(); List<Long> ids=new ArrayList<Long>();
        for(RepoItemRelation relation:relations){Long id=item.getId().equals(relation.getItemId())?relation.getRelatedItemId():relation.getItemId();if(id!=null&&!ids.contains(id))ids.add(id);}
        String subjects=safe(item.getSubjects());
        if(ids.isEmpty()&&subjects.length()>0){List<RepoItem> candidates=publicCriteria(session,null).add(Restrictions.ne("id",item.getId())).add(Restrictions.ilike("subjects",firstToken(subjects),MatchMode.ANYWHERE)).setMaxResults(maximum).list();return cards(session,candidates);}
        if(ids.isEmpty())return new ArrayList<ItemCard>(); List<RepoItem> rows=publicCriteria(session,null).add(Restrictions.in("id",ids)).setMaxResults(maximum).list();return cards(session,rows);
    }

    @SuppressWarnings("unchecked")
    private List<ItemCard> versionItems(Session session, RepoItem item, int maximum) {
        org.hibernate.criterion.Disjunction any=Restrictions.disjunction(); any.add(Restrictions.eq("id",item.getId()));
        any.add(Restrictions.eq("previousVersionId",item.getId())); if(item.getPreviousVersionId()!=null)any.add(Restrictions.eq("id",item.getPreviousVersionId()));
        if(item.getSourceClass()!=null&&item.getSourceId()!=null)any.add(Restrictions.and(Restrictions.eq("sourceClass",item.getSourceClass()),Restrictions.eq("sourceId",item.getSourceId())));
        List<RepoItem> rows=publicCriteria(session,null).add(any).addOrder(Order.asc("versionNumber")).setMaxResults(maximum).list();return cards(session,rows);
    }

    private List<ItemCard> cards(Session session,List<RepoItem> rows){List<ItemCard> out=new ArrayList<ItemCard>();Map<Long,RepoCollection> map=loadCollectionMap(session,rows);for(RepoItem row:rows)out.add(toCard(row,map.get(row.getCollectionId())));enrichCards(session,out);return out;}

    @SuppressWarnings("unchecked")
    private void enrichCards(Session session, List<ItemCard> cards) {
        if (cards == null || cards.isEmpty()) return;
        List<Long> ids = new ArrayList<Long>(); Map<Long, ItemCard> byId = new HashMap<Long, ItemCard>();
        for (ItemCard card : cards) { ids.add(card.id); byId.put(card.id, card); }
        List<RepoItemMetadata> metadata = session.createCriteria(RepoItemMetadata.class)
                .add(Restrictions.in("itemId", ids)).add(activeRestriction())
                .add(Restrictions.eq("metadataField", "repository.programStudy"))
                .addOrder(Order.asc("place")).addOrder(Order.asc("id")).list();
        for (RepoItemMetadata row : metadata) {
            ItemCard card = byId.get(row.getItemId());
            if (card != null && card.programStudy.length() == 0) card.programStudy = safe(row.getMetadataValue());
        }
        List<RepoBitstream> files = session.createCriteria(RepoBitstream.class)
                .add(Restrictions.in("itemId", ids)).add(activeRestriction())
                .add(Restrictions.eq("accessPolicy", "OPEN_ACCESS")).list();
        for (RepoBitstream file : files) {
            ItemCard card = byId.get(file.getItemId());
            if (card == null || !"OPEN_ACCESS".equals(normalizeAccess(card.accessPolicy))) continue;
            card.publicFileCount++;
            String mime = safe(file.getMimeType()).toLowerCase(); String name = safe(file.getNamaFile()).toLowerCase();
            if (mime.indexOf("pdf") >= 0 || name.endsWith(".pdf")) card.pdfAvailable = true;
        }
        List<RepoItemContributor> contributors=session.createCriteria(RepoItemContributor.class)
                .add(Restrictions.in("itemId",ids)).add(activeRestriction()).addOrder(Order.asc("sequenceNumber")).list();
        List<Long> authorityIds=new ArrayList<Long>();for(RepoItemContributor contributor:contributors)if(!authorityIds.contains(contributor.getAuthorityId()))authorityIds.add(contributor.getAuthorityId());
        Map<Long,RepoAuthorAuthority> authorities=new HashMap<Long,RepoAuthorAuthority>();if(!authorityIds.isEmpty()){
            List<RepoAuthorAuthority> authorityRows=session.createCriteria(RepoAuthorAuthority.class).add(Restrictions.in("id",authorityIds)).add(activeRestriction()).list();
            for(RepoAuthorAuthority authority:authorityRows)authorities.put(authority.getId(),authority);
        }
        for(RepoItemContributor contributor:contributors){ItemCard card=byId.get(contributor.getItemId());RepoAuthorAuthority authority=authorities.get(contributor.getAuthorityId());
            if(card!=null&&authority!=null&&authority.getOrcid().length()>0)card.authorOrcids+=(card.authorOrcids.length()==0?"":";")+authority.getOrcid();}
        List<Object> replaced=session.createCriteria(RepoItem.class).add(tenantRestriction()).add(publicVisibilityRestriction())
                .add(Restrictions.in("previousVersionId",ids)).setProjection(Projections.distinct(Projections.property("previousVersionId"))).list();
        for(Object id:replaced)if(id instanceof Long&&byId.get((Long)id)!=null)byId.get((Long)id).superseded=true;
    }
    private static String firstToken(String value){String[]v=safe(value).split("[;,]");return v.length==0?"":clean(v[0]);}
    private static void increment(Map<String,Long> map,String key){Long n=map.get(key);map.put(key,Long.valueOf(n==null?1L:n.longValue()+1L));}

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

    private String normalizeSearchField(String value) {
        String field = clean(value).toLowerCase();
        return "title".equals(field) || "author".equals(field) || "subject".equals(field)
                || "abstract".equals(field) || "program".equals(field) || "advisor".equals(field)
                || "fulltext".equals(field) || "identifier".equals(field) ? field : "all";
    }

    private String normalizeFullText(String value) {
        String fullText = clean(value).toUpperCase();
        return "WITH_FILE".equals(fullText) || "METADATA_ONLY".equals(fullText) ? fullText : "";
    }

    private List<String> synonymTerms(String keyword) {
        List<String> result = new ArrayList<String>(); String original = clean(keyword);
        if (original.length() == 0) return result; result.add(original);
        String configured = System.getProperty("ais.repository.searchSynonyms",
                "umkm=usaha mikro kecil menengah|usaha kecil;skripsi=tugas akhir|thesis;dosen=tenaga pengajar;mahasiswa=pelajar|peserta didik");
        String lower = original.toLowerCase();
        for (String group : configured.split(";")) {
            int equals = group.indexOf('='); if (equals <= 0) continue;
            String key = clean(group.substring(0, equals)).toLowerCase();
            if (key.length() == 0 || lower.indexOf(key) < 0) continue;
            for (String replacement : group.substring(equals + 1).split("\\|")) {
                String expanded = clean(lower.replace(key, clean(replacement).toLowerCase()));
                if (expanded.length() > 0 && !result.contains(expanded)) result.add(expanded);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private String suggestCorrection(Session session, String keyword) {
        List<Object[]> rows = session.createCriteria(RepoItem.class).add(publicVisibilityRestriction()).add(tenantRestriction())
                .setProjection(Projections.projectionList().add(Projections.property("title"))
                        .add(Projections.property("authors")).add(Projections.property("subjects")))
                .setMaxResults(1500).list();
        Map<String, Boolean> vocabulary = new LinkedHashMap<String, Boolean>();
        for (Object[] row : rows) for (Object value : row) for (String token : safe(value == null ? "" : String.valueOf(value)).split("[^\\p{L}\\p{N}]+"))
            if (token.length() >= 3 && vocabulary.size() < 12000) vocabulary.put(token.toLowerCase(), Boolean.TRUE);
        String[] input = clean(keyword).toLowerCase().split("\\s+"); boolean changed = false;
        for (int i = 0; i < input.length; i++) {
            if (input[i].length() < 3 || vocabulary.containsKey(input[i])) continue;
            String best = input[i]; int bestDistance = Math.min(3, Math.max(1, input[i].length() / 3));
            for (String candidate : vocabulary.keySet()) {
                if (Math.abs(candidate.length() - input[i].length()) > bestDistance) continue;
                int distance = editDistance(input[i], candidate, bestDistance);
                if (distance < bestDistance) { best = candidate; bestDistance = distance; if (distance == 1) break; }
            }
            if (!best.equals(input[i])) { input[i] = best; changed = true; }
        }
        if (!changed) return ""; StringBuilder corrected = new StringBuilder();
        for (String token : input) { if (corrected.length() > 0) corrected.append(' '); corrected.append(token); }
        return corrected.toString();
    }

    private int editDistance(String left, String right, int stopAfter) {
        int[] previous = new int[right.length() + 1]; for (int j = 0; j <= right.length(); j++) previous[j] = j;
        for (int i = 1; i <= left.length(); i++) {
            int[] current = new int[right.length() + 1]; current[0] = i; int rowMin = current[0];
            for (int j = 1; j <= right.length(); j++) { int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost); rowMin = Math.min(rowMin, current[j]); }
            if (rowMin > stopAfter) return rowMin; previous = current;
        }
        return previous[right.length()];
    }

    private String reversedName(String value) {
        String name = clean(value); int comma = name.indexOf(',');
        if (comma > 0) return clean(name.substring(comma + 1)) + " " + clean(name.substring(0, comma));
        int space = name.lastIndexOf(' '); return space > 0 ? clean(name.substring(space + 1)) + ", " + clean(name.substring(0, space)) : name;
    }

    private String normalizePreferenceType(String value) {
        String type=clean(value).toUpperCase();return "BOOKMARK".equals(type)||"SAVED_SEARCH".equals(type)||"SEARCH_ALERT".equals(type)?type:"";
    }

    private String normalizeSort(String value) {
        String sort = clean(value).toLowerCase();
        return "oldest".equals(sort) || "title".equals(sort) || "author".equals(sort) ? sort : "newest";
    }

    private String citationEscape(String value) {
        return safe(value).replace("\\", "\\\\").replace("{", "\\{").replace("}", "\\}");
    }
    private String citationIdentifier(ItemDetail item) {
        return clean(item.doi).length() > 0 ? "https://doi.org/" + clean(item.doi)
                : (clean(item.dspaceHandle).length() > 0 ? item.dspaceHandle : item.oaiIdentifier);
    }
    private static String xmlEscape(String value){return safe(value).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}

    public void recordUsage(Long itemId, Long bitstreamId, String eventType, String visitor, String userAgent, String actorId) {
        recordUsage(itemId,bitstreamId,eventType,visitor,userAgent,actorId,"","");
    }
    public void recordUsage(Long itemId, Long bitstreamId, String eventType, String visitor, String userAgent, String actorId,
            String countryCode,String referrer) {
        if (itemId == null || !("VIEW".equals(eventType) || "DOWNLOAD".equals(eventType))) return;
        Session s = session();
        org.hibernate.Transaction tx = null;
        try {
            tx = s.beginTransaction();

            /*
             * Jangan load/update RepoItem sebagai entity di sini. Data repository lama
             * dapat mempunyai lock_version NULL setelah kolom dibuat oleh hbm2ddl;
             * Hibernate 3 akan NPE di LongType.next() sebelum sempat menulis perubahan.
             * Bulk DML ini atomik untuk akses bersamaan, sekaligus memperbaiki versi NULL
             * pada record yang sedang dikunjungi tanpa ALTER tabel manual.
             */
            String agentClass=userAgentClass(userAgent);int updated=1;
            if(!"BOT".equals(agentClass)){String counterColumn = "VIEW".equals(eventType) ? "view_count" : "download_count";
                updated = s.createSQLQuery("update public.repo_item set " + counterColumn
                        + " = coalesce(" + counterColumn + ", 0) + 1, lock_version = coalesce(lock_version, 0) where id = :itemId")
                        .setLong("itemId", itemId.longValue()).executeUpdate();}
            if (updated == 0) {
                tx.rollback();
                return;
            }

            RepoUsageEvent e = new RepoUsageEvent();
            e.setItemId(itemId);
            e.setBitstreamId(bitstreamId);
            e.setEventType(eventType);
            e.setVisitorHash(hash(visitor));
            e.setActorId(clean(actorId));
            e.setUserAgentClass(agentClass);e.setCountryCode(limit(clean(countryCode).toUpperCase(),8));e.setReferrerHost(referrerHost(referrer));
            e.setOccurredAt(new Date());
            s.save(e);
            tx.commit();
        } catch (Exception ex) {
            if (tx != null && tx.isActive()) try { tx.rollback(); } catch (Exception ignored) {}
            ais.common.ErrorAuditUtil.record(ex, "RepositoryPublicService.recordUsage");
        }
    }
    private static String hash(String value){try{String salt=System.getProperty("ais.repository.analyticsSalt","AIS-REPOSITORY");byte[]b=MessageDigest.getInstance("SHA-256").digest((salt+"|"+clean(value)).getBytes("UTF-8"));StringBuilder x=new StringBuilder();for(byte v:b)x.append(String.format("%02x",v&255));return x.toString();}catch(Exception e){return "";}}
    private static String userAgentClass(String value){String v=clean(value).toLowerCase();if(v.contains("bot")||v.contains("crawler")||v.contains("spider"))return "BOT";if(v.contains("mobile"))return "MOBILE";return "DESKTOP";}
    private static String referrerHost(String value){try{String host=new java.net.URL(clean(value)).getHost();return limit(clean(host).toLowerCase(),255);}catch(Exception e){return "";}}

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
