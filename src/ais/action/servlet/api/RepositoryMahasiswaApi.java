package ais.action.servlet.api;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.repository.RepoCollection;
import ais.database.model.repository.RepoItem;

/**
 * API baca Repository untuk portal mahasiswa Android/Desktop.
 *
 * <p>Query dan urutan data mengikuti {@code RepositoryAction}: item aktif,
 * pencarian pada judul/penulis/subjek, item terbaru lebih dahulu, dan koleksi
 * berdasarkan urutan/nama. Operasi sinkronisasi serta import SQL sengaja tidak
 * diekspos kepada mahasiswa karena merupakan privilege administrator.</p>
 */
public final class RepositoryMahasiswaApi {
    private static final int MAX_PAGE_SIZE = 100;

    private RepositoryMahasiswaApi() { }

    @SuppressWarnings("unchecked")
    public static JSONObject daftar(HttpServletRequest req, JSONObject request) {
        Tbmuser caller = ApiUtil.currentUser(request, req);
        if (caller == null || caller.getMahasiswa() == null || caller.getMahasiswa().getId() == null) {
            return ApiHelperSupport.status("97", "Token mahasiswa tidak valid atau sudah berakhir.");
        }

        int page = nonNegative(integer(request, "halaman", 0));
        int size = Math.min(MAX_PAGE_SIZE, Math.max(10, integer(request, "jumlahDataDalamSatuHalaman", 25)));
        String query = ApiHelperSupport.optString(request, "q").trim();
        Session session = null;
        try {
            session = HibernateUtil.openSession();
            Criteria itemCriteria = itemCriteria(session, query);
            Number total = (Number) itemCriteria.setProjection(Projections.rowCount()).uniqueResult();

            List<RepoItem> items = itemCriteria(session, query)
                    .addOrder(Order.desc("lastSyncAt"))
                    .addOrder(Order.desc("id"))
                    .setFirstResult(page * size)
                    .setMaxResults(size)
                    .list();
            List<RepoCollection> collections = session.createCriteria(RepoCollection.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
                    .addOrder(Order.asc("sortOrder"))
                    .addOrder(Order.asc("nama"))
                    .list();

            JSONObject result = ApiHelperSupport.status("00", "Data repository berhasil dimuat.");
            result.put("data", items(items));
            result.put("koleksi", collections(collections));
            result.put("size", total == null ? 0 : total.intValue());
            result.put("halaman", page);
            result.put("ringkasan", summary(session));
            return result;
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            return ApiHelperSupport.errorResponse("Data repository gagal dimuat.");
        } finally {
            ApiHelperSupport.closeOpenedSession(session);
        }
    }

    private static Criteria itemCriteria(Session session, String query) {
        Criteria criteria = session.createCriteria(RepoItem.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
        if (ApiHelperSupport.hasText(query)) {
            criteria.add(Restrictions.or(
                    Restrictions.ilike("title", query, MatchMode.ANYWHERE),
                    Restrictions.or(
                            Restrictions.ilike("authors", query, MatchMode.ANYWHERE),
                            Restrictions.ilike("subjects", query, MatchMode.ANYWHERE))));
        }
        return criteria;
    }

    private static JSONArray items(List<RepoItem> rows) throws Exception {
        JSONArray array = new JSONArray();
        for (RepoItem row : rows) {
            JSONObject value = new JSONObject();
            value.put("id", row.getId());
            value.put("judul", row.getTitle());
            value.put("penulis", row.getAuthors());
            value.put("subjek", row.getSubjects());
            value.put("sumber", row.getSourceLabel());
            value.put("jenis", row.getDocumentType());
            value.put("akses", row.getAccessPolicy());
            value.put("statusSinkron", row.getSyncStatus());
            value.put("pesanSinkron", row.getSyncMessage());
            value.put("handle", row.getDspaceHandle());
            value.put("turnitin", row.getTurnitinIndexed());
            value.put("terakhirSinkron", row.getLastSyncAt() == null
                    ? JSONObject.NULL : row.getLastSyncAt().getTime());
            array.put(value);
        }
        return array;
    }

    private static JSONArray collections(List<RepoCollection> rows) throws Exception {
        JSONArray array = new JSONArray();
        for (RepoCollection row : rows) {
            JSONObject value = new JSONObject();
            value.put("id", row.getId());
            value.put("kode", row.getKode());
            value.put("nama", row.getNama());
            value.put("deskripsi", row.getDeskripsi());
            value.put("tipe", row.getTipe());
            value.put("sumber", row.getSourceSystem());
            value.put("handle", row.getDspaceHandle());
            array.put(value);
        }
        return array;
    }

    private static JSONObject summary(Session session) throws Exception {
        JSONObject value = new JSONObject();
        value.put("item", count(session, RepoItem.class, null, null));
        value.put("koleksi", count(session, RepoCollection.class, null, null));
        value.put("tersinkron", count(session, RepoItem.class, "syncStatus", "SYNCED"));
        value.put("gagal", count(session, RepoItem.class, "syncStatus", "FAILED"));
        value.put("draft", count(session, RepoItem.class, "syncStatus", "DRAFT"));
        value.put("turnitin", count(session, RepoItem.class, "turnitinIndexed", Boolean.TRUE));
        return value;
    }

    private static long count(Session session, Class<?> type, String property, Object expected) {
        Criteria criteria = session.createCriteria(type)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
        if (property != null) criteria.add(Restrictions.eq(property, expected));
        Number total = (Number) criteria.setProjection(Projections.rowCount()).uniqueResult();
        return total == null ? 0L : total.longValue();
    }

    private static int integer(JSONObject request, String key, int fallback) {
        try { return Integer.parseInt(ApiHelperSupport.optString(request, key)); }
        catch (Exception ignored) { return fallback; }
    }

    private static int nonNegative(int value) { return value < 0 ? 0 : value; }
}
