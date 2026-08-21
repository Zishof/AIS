package ais.action.master.library.modern;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.newui.NewUiCsrfUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.library.Anggota;

/** Read-only typed workspace endpoints for dashboards, visits, and circulation activity. */
public final class LibraryWorkspaceApi {
    private LibraryWorkspaceApi() { }

    public static JSONObject handle(HttpServletRequest request) throws JSONException {
        String action = request.getParameter("action");
        Context context = context(request);
        JSONObject result;
        if ("dashboard".equals(action)) result=dashboard(context);
        else if ("visits".equals(action)) result=visits(context, request);
        else if ("circulation".equals(action)) result=circulation(context, request);
        else result=new JSONObject().put("ok", false).put("error", "Operasi workspace tidak dikenal.");
        result.put("csrf",NewUiCsrfUtil.getToken(request.getSession()));
        return result;
    }

    private static JSONObject dashboard(Context context) throws JSONException {
        if (context.user != null && !context.admin && context.memberId == null) {
            return new JSONObject().put("ok", false).put("error", "Akun ini tidak memiliki akses dashboard perpustakaan.");
        }
        Session session = null;
        try {
            session = HibernateUtil.openSession();
            long titles = number(session.createSQLQuery("select count(id) from library.item where aktif is null or aktif=true").uniqueResult());
            long copies = number(session.createSQLQuery("select count(id) from library.item_punya_barcode").uniqueResult());
            long visits = scopedCount(session, "select count(k.id) from library.kunjungan_anggota k", "k.anggota", context);
            long loans = scopedCount(session, "select count(p.id) from library.peminjaman_pengadaan_item p where p.tanggal_persetujuan is not null", "p.anggota", context);
            long returns = scopedCount(session,
                    "select count(k.id) from library.kembali_pengadaan_item k join library.peminjaman_pengadaan_item p on p.id=k.peminjaman_pengadaan_item where k.tanggal_persetujuan is not null",
                    "p.anggota", context);
            long overdue = scopedCount(session,
                    "select count(d.id) from library.peminjaman_pengadaan_item_detail d join library.peminjaman_pengadaan_item p on p.id=d.peminjaman_pengadaan_item where d.batas_waktu_pengembalian < current_timestamp and d.kembali_pengadaan_item_detail is null",
                    "p.anggota", context);
            long holds = scopedCount(session,
                    "select count(h.id) from library.pesanan_anggota h where lower(coalesce(h.status,'')) not in ('batal','selesai','diambil')",
                    "h.anggota", context);

            JSONObject counts = new JSONObject().put("titles", titles).put("copies", copies).put("visits", visits)
                    .put("loans", loans).put("returns", returns).put("overdue", overdue).put("holds", holds);
            JSONObject result = new JSONObject().put("ok", true).put("scope", context.scope()).put("counts", counts);
            result.put("visitTrend", visitTrend(session, context));
            result.put("topSubjects", topSubjects(session));
            result.put("popularQueries", context.admin ? popularQueries(session) : new JSONArray());
            result.put("popularTitles", popularTitles(session));
            return result;
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    @SuppressWarnings("unchecked")
    private static JSONArray visitTrend(Session session, Context context) throws JSONException {
        String sql = "select to_char(k.tanggal,'YYYY-MM-DD'),count(k.id) from library.kunjungan_anggota k where k.tanggal >= current_date - interval '6 days'";
        if (context.memberOnly()) sql += " and k.anggota=:memberId";
        sql += " group by to_char(k.tanggal,'YYYY-MM-DD') order by 1";
        Query query = session.createSQLQuery(sql);
        if (context.memberOnly()) query.setLong("memberId", context.memberId.longValue());
        JSONArray result = new JSONArray();
        for (Object[] row : (List<Object[]>) query.list()) {
            result.put(new JSONObject().put("date", string(row[0])).put("count", number(row[1])));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static JSONArray topSubjects(Session session) throws JSONException {
        List<Object[]> rows = session.createSQLQuery("select kategories,count(id) from library.item where aktif=true and trim(coalesce(kategories,''))<>'' group by kategories order by count(id) desc limit 8").list();
        JSONArray result = new JSONArray();
        for (Object[] row : rows) result.put(new JSONObject().put("name", string(row[0])).put("count", number(row[1])));
        return result;
    }

    @SuppressWarnings("unchecked")
    private static JSONArray popularQueries(Session session) throws JSONException {
        List<Object[]> rows = session.createSQLQuery("select trim(text_query),count(id) from library.search_history where trim(coalesce(text_query,''))<>'' and trim(text_query)<>'__ALL__' and coalesce(text_result,'') not like '[REMOVED]%' group by trim(text_query) order by count(id) desc limit 8").list();
        JSONArray result = new JSONArray();
        for (Object[] row : rows) result.put(new JSONObject().put("name", string(row[0])).put("count", number(row[1])));
        return result;
    }

    @SuppressWarnings("unchecked")
    private static JSONArray popularTitles(Session session) throws JSONException {
        List<Object[]> rows = session.createSQLQuery("select id,coalesce(nama,'-'),coalesce(jumlahDilihat,0) from library.item where (aktif is null or aktif=true) and status_terbit_item in (select id from library.status_terbit_item where lower(trim(nama)) in ('terbit','publish','published')) order by coalesce(jumlahDilihat,0) desc,id desc limit 8").list();
        JSONArray result = new JSONArray();
        for (Object[] row : rows) result.put(new JSONObject().put("id", number(row[0])).put("name", string(row[1])).put("count", number(row[2])));
        return result;
    }

    @SuppressWarnings("unchecked")
    private static JSONObject visits(Context context, HttpServletRequest request) throws JSONException {
        Session session = null;
        try {
            session = HibernateUtil.openSession();
            int page = bounded(request.getParameter("page"), 1, 100000, 1);
            int size = bounded(request.getParameter("pageSize"), 1, 50, 20);
            Long libraryId = positiveLong(request.getParameter("libraryId"));
            String keyword = text(request.getParameter("query"), 100);

            if (context.user == null) {
                Query aggregate = session.createSQLQuery("select to_char(k.tanggal,'YYYY-MM-DD'),coalesce(p.nama,'-'),count(k.id) from library.kunjungan_anggota k left join library.perpustakaan p on p.id=k.perpustakaan group by to_char(k.tanggal,'YYYY-MM-DD'),p.nama order by 1 desc");
                aggregate.setFirstResult((page - 1) * size).setMaxResults(size);
                JSONArray rows = new JSONArray();
                for (Object[] row : (List<Object[]>) aggregate.list()) rows.put(new JSONObject().put("date", string(row[0])).put("library", string(row[1])).put("count", number(row[2])));
                return new JSONObject().put("ok", true).put("scope", "aggregate").put("data", rows).put("page", page).put("pageSize", size);
            }
            if (!context.admin && context.memberId == null) {
                return new JSONObject().put("ok", false).put("error", "Akun ini tidak memiliki akses riwayat kunjungan.");
            }

            String from = " from library.kunjungan_anggota k left join library.anggota a on a.id=k.anggota left join library.perpustakaan p on p.id=k.perpustakaan where 1=1";
            if (context.memberOnly()) from += " and k.anggota=:memberId";
            if (libraryId != null) from += " and k.perpustakaan=:libraryId";
            if (keyword != null && context.admin) from += " and (lower(coalesce(a.nama,'')) like :keyword or lower(coalesce(a.kode,'')) like :keyword)";
            Query countQuery = session.createSQLQuery("select count(k.id)" + from);
            bind(countQuery, context, libraryId, keyword);
            long total = number(countQuery.uniqueResult());
            Query dataQuery = session.createSQLQuery("select k.id,to_char(k.tanggal,'YYYY-MM-DD HH24:MI'),coalesce(p.nama,'-'),coalesce(k.keterangan,'-'),coalesce(a.kode,'-'),coalesce(a.nama,'-')" + from + " order by k.tanggal desc");
            bind(dataQuery, context, libraryId, keyword);
            dataQuery.setFirstResult((page - 1) * size).setMaxResults(size);
            JSONArray data = new JSONArray();
            for (Object[] row : (List<Object[]>) dataQuery.list()) {
                JSONObject item = new JSONObject().put("id", number(row[0])).put("date", string(row[1])).put("library", string(row[2])).put("note", string(row[3]));
                if (context.admin) item.put("memberCode", string(row[4])).put("memberName", string(row[5]));
                data.put(item);
            }
            return new JSONObject().put("ok", true).put("scope", context.scope()).put("total", total).put("page", page).put("pageSize", size).put("data", data);
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    @SuppressWarnings("unchecked")
    private static JSONObject circulation(Context context, HttpServletRequest request) throws JSONException {
        if (context.user == null) return new JSONObject().put("ok", false).put("error", "Silakan masuk untuk melihat aktivitas sirkulasi.");
        if (!context.admin && context.memberId == null) return new JSONObject().put("ok", false).put("error", "Akun ini tidak memiliki akses aktivitas sirkulasi.");
        Session session = null;
        try {
            session = HibernateUtil.openSession();
            int page = bounded(request.getParameter("page"), 1, 100000, 1);
            int size = bounded(request.getParameter("pageSize"), 1, 50, 20);
            String union = "(select 'Peminjaman' jenis,p.tanggal_persetujuan tanggal,p.anggota anggota_id,d.item item_id from library.peminjaman_pengadaan_item_detail d join library.peminjaman_pengadaan_item p on p.id=d.peminjaman_pengadaan_item where p.tanggal_persetujuan is not null union all select 'Pengembalian',k.tanggal_persetujuan,p.anggota,d.item from library.kembali_pengadaan_item_detail d join library.kembali_pengadaan_item k on k.id=d.kembali_pengadaan_item join library.peminjaman_pengadaan_item p on p.id=k.peminjaman_pengadaan_item where k.tanggal_persetujuan is not null) x";
            String from = " from " + union + " join library.item i on i.id=x.item_id join library.anggota a on a.id=x.anggota_id";
            if (context.memberOnly()) from += " where x.anggota_id=:memberId";
            Query countQuery = session.createSQLQuery("select count(*)" + from);
            if (context.memberOnly()) countQuery.setLong("memberId", context.memberId.longValue());
            long total = number(countQuery.uniqueResult());
            Query dataQuery = session.createSQLQuery("select x.jenis,to_char(x.tanggal,'YYYY-MM-DD HH24:MI'),i.id,coalesce(i.nama,'-'),coalesce(i.pengarangs,'-'),coalesce(a.kode,'-'),coalesce(a.nama,'-')" + from + " order by x.tanggal desc");
            if (context.memberOnly()) dataQuery.setLong("memberId", context.memberId.longValue());
            dataQuery.setFirstResult((page - 1) * size).setMaxResults(size);
            JSONArray data = new JSONArray();
            for (Object[] row : (List<Object[]>) dataQuery.list()) {
                JSONObject item = new JSONObject().put("type", string(row[0])).put("date", string(row[1])).put("itemId", number(row[2])).put("title", string(row[3])).put("authors", string(row[4]));
                if (context.admin) item.put("memberCode", string(row[5])).put("memberName", string(row[6]));
                data.put(item);
            }
            return new JSONObject().put("ok", true).put("scope", context.scope()).put("total", total).put("page", page).put("pageSize", size).put("data", data);
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    private static long scopedCount(Session session, String sql, String memberColumn, Context context) {
        if (context.memberOnly()) sql += (sql.toLowerCase().contains(" where ") ? " and " : " where ") + memberColumn + "=:memberId";
        Query query = session.createSQLQuery(sql);
        if (context.memberOnly()) query.setLong("memberId", context.memberId.longValue());
        return number(query.uniqueResult());
    }

    private static void bind(Query query, Context context, Long libraryId, String keyword) {
        if (context.memberOnly()) query.setLong("memberId", context.memberId.longValue());
        if (libraryId != null) query.setLong("libraryId", libraryId.longValue());
        if (keyword != null && context.admin) query.setString("keyword", "%" + keyword.toLowerCase() + "%");
    }

    private static Context context(HttpServletRequest request) {
        Tbmuser user = Common.getCurrentUser(request);
        boolean admin = LibraryPermissionGuard.isStaff(request);
        Long memberId = null;
        Session session = null;
        try {
            if (user != null) {
                session = HibernateUtil.openSession();
                Anggota member = (Anggota) session.createCriteria(Anggota.class).add(Restrictions.eq("tbmuser", user)).setMaxResults(1).uniqueResult();
                if (member != null) memberId = member.getId();
            }
        } catch (Exception ignored) {
            memberId = null;
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
        return new Context(user, memberId, admin);
    }

    private static String text(String raw, int max) { if (raw == null || raw.trim().length() == 0) return null; String value=raw.trim(); return value.length()>max?value.substring(0,max):value; }
    private static Long positiveLong(String raw) { try { long value=Long.parseLong(raw); return value>0?Long.valueOf(value):null; } catch(Exception ignored){ return null; } }
    private static int bounded(String raw,int min,int max,int fallback){ try{int value=Integer.parseInt(raw);return value<min||value>max?fallback:value;}catch(Exception ignored){return fallback;} }
    private static long number(Object value) { return value instanceof Number ? ((Number)value).longValue() : 0L; }
    private static String string(Object value) { return value == null ? "" : String.valueOf(value); }

    private static final class Context {
        private final Tbmuser user; private final Long memberId; private final boolean admin;
        private Context(Tbmuser user, Long memberId, boolean admin){this.user=user;this.memberId=memberId;this.admin=admin;}
        private boolean memberOnly(){return !admin && memberId!=null;}
        private String scope(){return admin?"staff":(memberId!=null?"member":"aggregate");}
    }
}
