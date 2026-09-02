package ais.common.newui.laporan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.newui.NewUiCsrfUtil;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Tbmuser;

/** Kontrak native baca-saja untuk tabel Akreditasi 1 B. */
public final class NewUiLaporanAkreditasi1BController {

    private static final String MODULE = "root/report";
    private static final String[] KOLOM = { "S3", "S2", "S1", "SP2", "SP1",
            "Profesi", "S3T", "S2T", "D4", "D3", "D2", "D1" };
    private static final String SQL =
            "select a.statusakreditasi, "
            + "sum(case when b.nama ilike 'S3' or b.nama ilike 'Strata 3' then 1 else 0 end), "
            + "sum(case when b.nama ilike 'S2' or b.nama ilike 'Strata 2' then 1 else 0 end), "
            + "sum(case when b.nama ilike 'S1' or b.nama ilike 'Strata 1' then 1 else 0 end), "
            + "sum(case when b.nama ilike 'Sp-2' then 1 else 0 end), "
            + "sum(case when b.nama ilike 'Sp-1' then 1 else 0 end), "
            + "sum(case when b.nama ilike 'Profesi' then 1 else 0 end), "
            + "sum(case when b.nama ilike 'S-3T' then 1 else 0 end), "
            + "sum(case when b.nama ilike 'S-2T' then 1 else 0 end), "
            + "sum(case when b.nama ilike 'D4' then 1 else 0 end), "
            + "sum(case when b.nama ilike 'D3' then 1 else 0 end), "
            + "sum(case when b.nama ilike 'D2' then 1 else 0 end), "
            + "sum(case when b.nama ilike 'D1' then 1 else 0 end) "
            + "from jurusan a inner join jenjang b on a.jenjang=b.id "
            + "where (a.aktif or a.aktif is null) "
            + "and a.statusakreditasi in (:status) ";

    private NewUiLaporanAkreditasi1BController() { }

    public static void handle(HttpServletRequest request, HttpServletResponse response,
            String pageKey) throws Exception {
        String action = text(request.getParameter("action"), "meta");
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        JSONObject json = new JSONObject();
        try {
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, pageKey, action)) {
                response.setStatus(403);
                fail(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia.");
                write(response, json); return;
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null) throw new SecurityException("Sesi tidak dikenal.");
            if ("meta".equals(action)) meta(json, request);
            else if ("lookup".equals(action)) lookup(json, request, user);
            else if ("export".equals(action)) export(json, request, user);
            else throw new IllegalArgumentException("Aksi tidak dikenal.");
            json.put("ok", true);
        } catch (SecurityException e) {
            response.setStatus(403); fail(json, "FORBIDDEN", e.getMessage());
        } catch (IllegalArgumentException e) {
            response.setStatus(422); fail(json, "VALIDATION_FAILED", e.getMessage());
        } catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Laporan Akreditasi 1 B gagal disiapkan.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiLaporanAkreditasi1BController"); }
            catch (Exception ignored) { }
        }
        write(response, json);
    }

    private static void meta(JSONObject json, HttpServletRequest request) throws Exception {
        JSONArray filter = new JSONArray();
        filter.put(relasi("fakultas", "Fakultas", Fakultas.class.getName(), false));
        filter.put(relasi("jurusan", "Prodi", Jurusan.class.getName(), false)
                .put("tergantungPada", "fakultas"));
        json.put("judul", "Laporan Akreditasi 1 B")
                .put("format", "pdf")
                .put("filter", filter)
                .put("csrfHeader", NewUiCsrfUtil.HEADER)
                .put("csrfToken", NewUiCsrfUtil.getToken(request.getSession(true)))
                .put("bolehUbah", false)
                .put("catatan", "Jumlah program studi per status akreditasi dan jenjang.");
    }

    private static JSONObject relasi(String nama, String label, String entity,
            boolean wajib) throws Exception {
        return new JSONObject().put("nama", nama).put("label", label)
                .put("tipe", "relasi").put("wajib", wajib).put("entity", entity);
    }

    @SuppressWarnings("unchecked")
    private static void lookup(JSONObject json, HttpServletRequest request, Tbmuser user)
            throws Exception {
        String filter = text(request.getParameter("filter"), "");
        String q = text(request.getParameter("q"), "");
        JSONArray pilihan = new JSONArray();
        Session session = HibernateUtil.openSession();
        try {
            if ("fakultas".equals(filter)) {
                Fakultas scope = user.ambilFakultas();
                Criteria c = session.createCriteria(Fakultas.class).setMaxResults(50);
                if (scope != null) c.add(Restrictions.eq("id", scope.getId()));
                if (q.length() >= 2) c.add(Restrictions.ilike("nama", "%" + q + "%"));
                c.addOrder(Order.asc("nama"));
                for (Fakultas row : (List<Fakultas>) c.list())
                    option(pilihan, row.getId(), row.getNama());
            } else if ("jurusan".equals(filter)) {
                Jurusan scope = user.ambilJurusan();
                Long fakultas = idOpsional(request.getParameter("fakultas"));
                Criteria c = session.createCriteria(Jurusan.class).setMaxResults(50);
                if (scope != null) c.add(Restrictions.eq("id", scope.getId()));
                else if (fakultas != null) c.createAlias("fakultas", "f")
                        .add(Restrictions.eq("f.id", fakultas));
                if (q.length() >= 2) c.add(Restrictions.ilike("nama", "%" + q + "%"));
                c.addOrder(Order.asc("nama"));
                for (Jurusan row : (List<Jurusan>) c.list())
                    option(pilihan, row.getId(), row.getNama());
            } else {
                throw new IllegalArgumentException("Filter relasi tidak dikenal.");
            }
        } finally { session.close(); }
        json.put("filter", filter).put("pilihan", pilihan).put("total", pilihan.length())
                .put("batas", 50);
    }

    private static void option(JSONArray array, Object id, String nama) throws Exception {
        array.put(new JSONObject().put("id", id).put("nama", text(nama, "-")));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void export(JSONObject json, HttpServletRequest request, Tbmuser user)
            throws Exception {
        Session session = HibernateUtil.openSession();
        try {
            Fakultas fakultas = entity(session, Fakultas.class,
                    idOpsional(request.getParameter("fakultas")));
            Jurusan jurusan = entity(session, Jurusan.class,
                    idOpsional(request.getParameter("jurusan")));
            Fakultas scopeF = user.ambilFakultas();
            Jurusan scopeJ = user.ambilJurusan();
            if (scopeF != null) {
                if (fakultas != null && !scopeF.getId().equals(fakultas.getId())) forbiddenScope();
                fakultas = entity(session, Fakultas.class, scopeF.getId());
            }
            if (scopeJ != null) {
                if (jurusan != null && !scopeJ.getId().equals(jurusan.getId())) forbiddenScope();
                jurusan = entity(session, Jurusan.class, scopeJ.getId());
            }
            if (fakultas != null && jurusan != null && jurusan.getFakultas() != null
                    && !fakultas.getId().equals(jurusan.getFakultas().getId())) {
                throw new IllegalArgumentException("Prodi tidak berada pada fakultas yang dipilih.");
            }
            List<Map<String, Object>> maps = ambil(session, fakultas, jurusan);
            Map parameters = ais.common.HashMapGenerator.getRand();
            parameters.put("fakultas", fakultas == null ? "" : fakultas.getNama());
            parameters.put("jurusan", jurusan == null ? "" : jurusan.getNama());
            parameters.put("maps", maps);
            JasperPdfUtil.tulis(json, "std9/1b", parameters,
                    "AKREDITASI_1_B", "Laporan Akreditasi 1 B");
        } finally { session.close(); }
    }

    private static void forbiddenScope() {
        throw new SecurityException("Pilihan berada di luar lingkup hak akses pengguna.");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> ambil(Session session, Fakultas fakultas,
            Jurusan jurusan) {
        String sql = SQL;
        if (fakultas != null) sql += "and a.fakultas=:fakultas ";
        if (jurusan != null) sql += "and a.id=:jurusan ";
        sql += "group by a.statusakreditasi";
        SQLQuery query = session.createSQLQuery(sql);
        query.setParameterList("status", Jurusan.SEMUA_STATUS);
        if (fakultas != null) query.setLong("fakultas", fakultas.getId());
        if (jurusan != null) query.setLong("jurusan", jurusan.getId());
        return susunBaris((List<Object[]>) query.list());
    }

    /** Susun urutan status tetap, termasuk baris nol yang tidak ditemukan kueri. */
    static List<Map<String, Object>> susunBaris(List<Object[]> agregat) {
        Map<String, Object[]> perStatus = new HashMap<String, Object[]>();
        if (agregat != null) for (Object[] row : agregat) {
            if (row != null && row.length > 0 && row[0] != null)
                perStatus.put(row[0].toString(), row);
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (String status : Jurusan.SEMUA_STATUS) {
            Object[] row = perStatus.get(status);
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("status", status);
            for (int i = 0; i < KOLOM.length; i++)
                data.put(KOLOM[i], Double.valueOf(angka(row, i + 1)));
            result.add(data);
        }
        return result;
    }

    private static double angka(Object[] row, int index) {
        Object value = row == null || row.length <= index ? null : row[index];
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return value == null ? 0.0 : Double.parseDouble(value.toString()); }
        catch (Exception e) { return 0.0; }
    }

    @SuppressWarnings("unchecked")
    private static <T> T entity(Session session, Class<T> type, Long id) {
        if (id == null) return null;
        Object value = session.get(type, id);
        if (value == null) throw new IllegalArgumentException("Data filter tidak ditemukan.");
        return (T) value;
    }

    private static Long idOpsional(String value) {
        String s = text(value, "");
        if (s.length() == 0 || "-1".equals(s)) return null;
        try {
            long id = Long.parseLong(s);
            if (id <= 0) throw new NumberFormatException();
            return Long.valueOf(id);
        } catch (Exception e) { throw new IllegalArgumentException("Id filter tidak sah."); }
    }

    private static String text(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value.trim();
    }

    private static void fail(JSONObject json, String code, String message) throws Exception {
        json.put("ok", false).put("code", code).put("error", text(message, code));
    }

    private static void write(HttpServletResponse response, JSONObject json) throws Exception {
        response.getWriter().write(json.toString());
    }
}
