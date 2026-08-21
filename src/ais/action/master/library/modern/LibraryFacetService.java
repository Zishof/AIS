package ais.action.master.library.modern;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Server-side discovery facets and bounded suggestions for the public catalog. */
public final class LibraryFacetService {
    private static final int FACET_LIMIT = 12;

    public JSONObject facets(Session session, LibraryCatalogSearchRequest request) {
        try {
            JSONObject result = new JSONObject();
            result.put("availability", availability(session, request));
            result.put("itemTypes", entityFacet(session, request, "jenisItem", "facetItemType"));
            result.put("materialTypes", entityFacet(session, request, "tipeItem", "facetMaterialType"));
            result.put("libraries", libraryFacet(session));
            result.put("languages", scalarFacet(session, request, "bahasa"));
            result.put("publishers", entityFacet(session, request, "penerbit", "facetPublisher"));
            result.put("subjects", scalarFacet(session, request, "kategories"));
            result.put("stats", statistics(session, request));
            return result;
        } catch (Exception error) {
            ais.common.Common.tampilErrorJikaAdmin(error);
            return new JSONObject();
        }
    }

    @SuppressWarnings("unchecked")
    public JSONArray suggestions(Session session, String keyword) throws JSONException {
        JSONArray result = new JSONArray();
        if (keyword == null || keyword.trim().length() < 2) return result;
        LibraryCatalogSearchRequest request = new LibraryCatalogSearchRequest();
        request.setQuery(keyword.trim());
        Criteria criteria = new LibraryCatalogSearchService().createCriteria(session, request);
        criteria.setProjection(Projections.projectionList().add(Projections.property("id"))
                .add(Projections.property("nama")).add(Projections.property("pengarangs")));
        for (Object[] row : (List<Object[]>) criteria.setMaxResults(8).list()) {
            result.put(new JSONObject().put("id", row[0]).put("title", safe(row[1])).put("authors", safe(row[2])));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private JSONArray entityFacet(Session session, LibraryCatalogSearchRequest request, String association, String alias)
            throws JSONException {
        Criteria criteria = new LibraryCatalogSearchService().createCriteria(session, request);
        criteria.createAlias(association, alias, Criteria.LEFT_JOIN);
        criteria.setProjection(Projections.projectionList()
                .add(Projections.groupProperty(alias + ".id"))
                .add(Projections.groupProperty(alias + ".nama"))
                .add(Projections.rowCount()));
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
        criteria.setProjection(Projections.projectionList().add(Projections.groupProperty(property)).add(Projections.rowCount()));
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
        Query query = session.createSQLQuery("select p.id,p.nama,count(distinct b.item) from library.item_punya_barcode b "
                + "join library.item i on i.id=b.item left join library.perpustakaan p on p.id=b.perpustakaan "
                + "where " + publicItem + " group by p.id,p.nama order by count(distinct b.item) desc");
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
        criteria.add(Restrictions.or(Restrictions.eq("bolehDiDownload", Boolean.TRUE),
                Restrictions.or(Restrictions.isNotNull("ebooksLink"), Restrictions.isNotNull("ebooksLinkPdf"))));
        return number(criteria.setProjection(Projections.rowCount()).uniqueResult());
    }

    private JSONObject statistics(Session session, LibraryCatalogSearchRequest request) throws JSONException {
        long titles = number(new LibraryCatalogSearchService().createCriteria(session, request)
                .setProjection(Projections.rowCount()).uniqueResult());
        long copies = number(session.createSQLQuery("select count(b.id) from library.item_punya_barcode b join library.item i on i.id=b.item "
                + "where (i.aktif is null or i.aktif=true) and i.status_terbit_item in "
                + "(select id from library.status_terbit_item where lower(trim(nama)) in ('terbit','publish','published')) ").uniqueResult());
        long branches = number(session.createSQLQuery("select count(id) from library.perpustakaan where aktif is null or aktif=true").uniqueResult());
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
