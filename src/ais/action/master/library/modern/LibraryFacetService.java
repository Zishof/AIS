package ais.action.master.library.modern;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Server-side discovery facets and bounded suggestions for the public catalog. */
public final class LibraryFacetService {
    private static final int FACET_LIMIT = 12;
    private static final long CACHE_MILLIS = 30000L;
    private static final ConcurrentHashMap<String, CacheEntry> CACHE = new ConcurrentHashMap<String, CacheEntry>();

    public JSONObject facets(Session session, LibraryCatalogSearchRequest request) {
        try {
            String cacheKey=cacheKey(request);CacheEntry cached=CACHE.get(cacheKey);long now=System.currentTimeMillis();
            if(cached!=null&&now-cached.created<CACHE_MILLIS)return new JSONObject(cached.json);
            JSONObject result = new JSONObject();
            result.put("availability", availability(session, request));
            result.put("itemTypes", entityFacet(session, request, "jenisItem", "facetItemType"));
            result.put("materialTypes", entityFacet(session, request, "tipeItem", "facetMaterialType"));
            result.put("libraries", libraryFacet(session));
            result.put("languages", scalarFacet(session, request, "bahasa"));
            result.put("authors", scalarFacet(session, request, "pengarangs"));
            result.put("publishers", entityFacet(session, request, "penerbit", "facetPublisher"));
            result.put("subjects", scalarFacet(session, request, "kategories"));
            result.put("years", scalarFacet(session, request, "tahun"));
            result.put("schools", entityFacet(session, request, "sekolah", "facetSchool"));
            result.put("studyPrograms", entityFacet(session, request, "jurusan", "facetStudyProgram"));
            result.put("popularSearches", scalarFacet(session, new LibraryCatalogSearchRequest(), "kategories"));
            result.put("stats", statistics(session, request));
            if(CACHE.size()>200)CACHE.clear();CACHE.put(cacheKey,new CacheEntry(now,result.toString()));
            return result;
        } catch (Exception error) {
            ais.common.Common.tampilErrorJikaAdmin(error);
            return new JSONObject();
        }
    }

    private String cacheKey(LibraryCatalogSearchRequest r){ais.database.model.library.Perpustakaan p=ais.common.Common.getCurrentPerpustakaan();return (p==null?"*":String.valueOf(p.getId()))+'|'+safe(r.getQuery())+'|'+safe(r.getSearchField())+'|'+safe(r.getMatchMode())+'|'+safe(r.getTitle())+'|'+safe(r.getAuthor())+'|'+safe(r.getPublisher())+'|'+safe(r.getSubject())+'|'+safe(r.getNotes())+'|'+safe(r.getExclude())+'|'+safe(r.getLanguage())+'|'+safe(r.getAvailability())+'|'+safe(r.getLibraryId())+'|'+safe(r.getItemTypeId())+'|'+safe(r.getMaterialTypeId())+'|'+safe(r.getSchoolId())+'|'+safe(r.getStudyProgramId())+'|'+safe(r.getYearFrom())+'|'+safe(r.getYearTo());}
    private static final class CacheEntry{private final long created;private final String json;private CacheEntry(long created,String json){this.created=created;this.json=json;}}

    @SuppressWarnings("unchecked")
    public JSONArray suggestions(Session session, String keyword) throws JSONException {
        JSONArray result = new JSONArray();
        if (keyword == null || keyword.trim().length() < 2) return result;
        LibraryCatalogSearchRequest request = new LibraryCatalogSearchRequest();
        request.setQuery(keyword.trim());
        LibraryScopeResolver.apply(request);
        Criteria criteria = new LibraryCatalogSearchService().createCriteria(session, request);
        criteria.setProjection(Projections.projectionList().add(Projections.property("id"))
                .add(Projections.property("nama")).add(Projections.property("pengarangs"))
                .add(Projections.property("kategories")).add(Projections.property("callnumber"))
                .add(Projections.property("isbn")));
        String needle = keyword.trim().toLowerCase();
        Set<String> seen = new LinkedHashSet<String>();
        JSONArray titles = new JSONArray(), authors = new JSONArray(), subjects = new JSONArray();
        JSONArray callNumbers = new JSONArray(), identifiers = new JSONArray();
        List<Object[]> rows=(List<Object[]>) criteria.setMaxResults(16).list();
        if(rows.isEmpty()&&needle.length()>3){request.setQuery(needle.substring(0,3));criteria=new LibraryCatalogSearchService().createCriteria(session,request);criteria.setProjection(Projections.projectionList().add(Projections.property("id")).add(Projections.property("nama")).add(Projections.property("pengarangs")).add(Projections.property("kategories")).add(Projections.property("callnumber")).add(Projections.property("isbn")));rows=(List<Object[]>)criteria.setMaxResults(24).list();}
        for (Object[] row : rows) {
            addSuggestion(titles, seen, "TITLE", safe(row[1]), safe(row[2]), row[0], needle);
            addSuggestion(authors, seen, "AUTHOR", safe(row[2]), "Penulis / pengarang", row[0], needle);
            addSuggestion(subjects, seen, "SUBJECT", firstValue(row[3]), "Subjek", row[0], needle);
            addSuggestion(callNumbers, seen, "CALL_NUMBER", safe(row[4]), "Nomor panggil", row[0], needle);
            addSuggestion(identifiers, seen, "ISBN", safe(row[5]), "ISBN", row[0], needle);
        }
        append(result, titles, 5); append(result, authors, 3); append(result, subjects, 2);
        append(result, callNumbers, 1); append(result, identifiers, 1);
        return result;
    }

    private void append(JSONArray target, JSONArray source, int maximum) throws JSONException {
        for (int i = 0; i < source.length() && i < maximum && target.length() < 12; i++) target.put(source.getJSONObject(i));
    }

    private void addSuggestion(JSONArray result, Set<String> seen, String type, String value,
            String meta, Object id, String needle) throws JSONException {
        if (result.length() >= 12 || blank(value) || !matches(value,needle)) return;
        String key = type + ":" + value.toLowerCase();
        if (!seen.add(key)) return;
        result.put(new JSONObject().put("id", id).put("type", type).put("value", value)
                .put("title", value).put("meta", meta).put("authors", meta));
    }

    private static boolean matches(String value,String needle){String lower=value.toLowerCase();if(lower.indexOf(needle)>=0)return true;for(String word:lower.split("[^a-z0-9]+"))if(word.length()>2&&distance(word,needle)<=2)return true;return false;}
    private static int distance(String a,String b){int[] previous=new int[b.length()+1];for(int j=0;j<=b.length();j++)previous[j]=j;for(int i=1;i<=a.length();i++){int[] current=new int[b.length()+1];current[0]=i;for(int j=1;j<=b.length();j++)current[j]=Math.min(Math.min(current[j-1]+1,previous[j]+1),previous[j-1]+(a.charAt(i-1)==b.charAt(j-1)?0:1));previous=current;}return previous[b.length()];}

    private static String firstValue(Object raw) {
        String value = safe(raw);
        if (value.length() == 0) return value;
        int comma = value.indexOf(',');
        int semicolon = value.indexOf(';');
        int split = comma < 0 ? semicolon : semicolon < 0 ? comma : Math.min(comma, semicolon);
        return split < 0 ? value : value.substring(0, split).trim();
    }

    @SuppressWarnings("unchecked")
    private JSONArray entityFacet(Session session, LibraryCatalogSearchRequest request, String association, String alias)
            throws JSONException {
        Criteria criteria = new LibraryCatalogSearchService().createCriteria(session, request);
        criteria.createAlias(association, alias, Criteria.LEFT_JOIN);
        criteria.setProjection(Projections.projectionList()
                .add(Projections.groupProperty(alias + ".id"))
                .add(Projections.groupProperty(alias + ".nama"))
                .add(Projections.rowCount(), "facetCount"));
        criteria.addOrder(Order.desc("facetCount")).setMaxResults(FACET_LIMIT);
        JSONArray result = new JSONArray();
        int added = 0;
        for (Object[] row : (List<Object[]>) criteria.list()) {
            if (row[0] == null || blank(row[1]) || added >= FACET_LIMIT) continue;
            result.put(new JSONObject().put("id", row[0]).put("name", safe(row[1])).put("count", number(row[2])));
            added++;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private JSONArray scalarFacet(Session session, LibraryCatalogSearchRequest request, String property)
            throws JSONException {
        Criteria criteria = new LibraryCatalogSearchService().createCriteria(session, request);
        criteria.setProjection(Projections.projectionList().add(Projections.groupProperty(property))
                .add(Projections.rowCount(), "facetCount"));
        criteria.addOrder(Order.desc("facetCount")).setMaxResults(FACET_LIMIT * 3);
        JSONArray result = new JSONArray();
        int added = 0;
        for (Object[] row : (List<Object[]>) criteria.list()) {
            if (blank(row[0]) || added >= FACET_LIMIT) continue;
            String name = safe(row[0]);
            if (name.indexOf(',') >= 0) name = name.substring(0, name.indexOf(',')).trim();
            result.put(new JSONObject().put("value", name).put("name", name).put("count", number(row[1])));
            added++;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private JSONArray libraryFacet(Session session) throws JSONException {
        String publicItem = "(i.aktif is null or i.aktif=true) and i.status_terbit_item in "
                + "(select id from library.status_terbit_item where lower(trim(nama)) in ('terbit','publish','published'))";
        List<Long> allowed=LibraryScopeResolver.allowedLibraryIds(session);
        if(allowed!=null&&allowed.isEmpty())return new JSONArray();
        Query query = session.createSQLQuery("select p.id,p.nama,count(distinct b.item) from library.item_punya_barcode b "
                + "join library.item i on i.id=b.item left join library.perpustakaan p on p.id=b.perpustakaan "
                + "where " + publicItem +(allowed==null?"":" and p.id in (:allowedLibraries)")+ " group by p.id,p.nama order by count(distinct b.item) desc");
        if(allowed!=null)query.setParameterList("allowedLibraries",allowed);
        query.setMaxResults(FACET_LIMIT);
        JSONArray result = new JSONArray();
        for (Object[] row : (List<Object[]>) query.list()) {
            if (row[0] != null) result.put(new JSONObject().put("id", row[0]).put("name", safe(row[1])).put("count", number(row[2])));
        }
        return result;
    }

    private JSONArray availability(Session session, LibraryCatalogSearchRequest request) throws JSONException {
        JSONArray result = new JSONArray();
        result.put(option("AVAILABLE", "Tersedia sekarang", countWith(session, request,
                "exists (select 1 from library.item_punya_barcode b where b.item={alias}.id and not exists "
                + "(select 1 from library.peminjaman_pengadaan_item_detail d where d.item_punya_barcode=b.id and d.kembali_pengadaan_item_detail is null))")));
        result.put(option("DIGITAL", "Koleksi digital", digitalCount(session, request)));
        result.put(option("LOANED", "Sedang dipinjam", countWith(session, request,
                "exists (select 1 from library.item_punya_barcode b join library.peminjaman_pengadaan_item_detail d "
                + "on d.item_punya_barcode=b.id where b.item={alias}.id and d.kembali_pengadaan_item_detail is null)")));
        return result;
    }

    private long countWith(Session session, LibraryCatalogSearchRequest request, String sql) {
        Criteria criteria = new LibraryCatalogSearchService().createCriteria(session, request);
        criteria.add(Restrictions.sqlRestriction(sql));
        Object value = criteria.setProjection(Projections.rowCount()).uniqueResult();
        return number(value);
    }

    private long digitalCount(Session session, LibraryCatalogSearchRequest request) {
        Criteria criteria = new LibraryCatalogSearchService().createCriteria(session, request);
        criteria.add(Restrictions.and(Restrictions.eq("bolehDiDownload", Boolean.TRUE),
                Restrictions.or(nonBlank("ebooksLink"), Restrictions.or(nonBlank("ebooksLinkPdf"), nonBlank("lampiranPath")))));
        return number(criteria.setProjection(Projections.rowCount()).uniqueResult());
    }

    private org.hibernate.criterion.Criterion nonBlank(String property) {
        return Restrictions.and(Restrictions.isNotNull(property), Restrictions.ne(property, ""));
    }

    private JSONObject statistics(Session session, LibraryCatalogSearchRequest request) throws JSONException {
        long titles = number(new LibraryCatalogSearchService().createCriteria(session, request)
                .setProjection(Projections.rowCount()).uniqueResult());
        List<Long> allowed = LibraryScopeResolver.allowedLibraryIds(session);
        Query copiesQuery = session.createSQLQuery("select count(b.id) from library.item_punya_barcode b join library.item i on i.id=b.item "
                + "where (i.aktif is null or i.aktif=true) and i.status_terbit_item in "
                + "(select id from library.status_terbit_item where lower(trim(nama)) in ('terbit','publish','published')) "
                + (allowed == null ? "" : "and b.perpustakaan in (:allowedLibraries)"));
        Query branchesQuery = session.createSQLQuery("select count(id) from library.perpustakaan where (aktif is null or aktif=true) "
                + (allowed == null ? "" : "and id in (:allowedLibraries)"));
        if (allowed != null) {
            if (allowed.isEmpty()) return new JSONObject().put("titles", titles).put("copies", 0).put("branches", 0)
                    .put("digital", digitalCount(session, request));
            copiesQuery.setParameterList("allowedLibraries", allowed);
            branchesQuery.setParameterList("allowedLibraries", allowed);
        }
        long copies = number(copiesQuery.uniqueResult());
        long branches = number(branchesQuery.uniqueResult());
        return new JSONObject().put("titles", titles).put("copies", copies).put("branches", branches)
                .put("digital", digitalCount(session, request));
    }

    private JSONObject option(String value, String name, long count) throws JSONException {
        return new JSONObject().put("value", value).put("name", name).put("count", count);
    }

    private static boolean blank(Object value) { return value == null || String.valueOf(value).trim().length() == 0; }
    private static String safe(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
    private static long number(Object value) { return value instanceof Number ? ((Number) value).longValue() : 0L; }
}
