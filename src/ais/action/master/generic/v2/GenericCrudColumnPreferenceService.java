package ais.action.master.generic.v2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.SQLQuery;

import ais.database.hibernate.HibernateUtil;

/** Preference user+active-role+entity persisted setelah migration 001, dengan fallback session aman. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class GenericCrudColumnPreferenceService {
    private static final String SESSION_PREFIX = "genericCrud.preference.";

    public Map load(GenericCrudRequestContext context) throws Exception {
        new GenericCrudPrivilegeGuard().require(context, GenericCrudOperation.READ);
        String user = userKey(context), role = roleKey(context), entity = context.getDefinition().getEntityKey();
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            SQLQuery q = session.createSQLQuery("select visible_columns_json,column_order_json,column_widths_json,pinned_columns_json,page_size,density,sort_json,filter_json,ui_state_json from generic_crud_user_view where user_key=:u and active_role_key=:r and entity_key=:e and view_name='default'");
            q.setString("u", user); q.setString("r", role); q.setString("e", entity); Object row = q.uniqueResult();
            if (row instanceof Object[]) return fromRow(context.getDefinition(), (Object[]) row);
        } catch (Exception migrationMissing) {
            Object fallback = context.getRequest().getSession(true).getAttribute(SESSION_PREFIX + role + "." + entity);
            if (fallback instanceof Map) return (Map) fallback;
        } finally { close(session); }
        return defaults(context.getDefinition());
    }

    public GenericCrudResult save(GenericCrudRequestContext context, Map requested) throws Exception {
        new GenericCrudPrivilegeGuard().require(context, GenericCrudOperation.READ);
        Map safe = sanitize(context.getDefinition(), requested);
        String user = userKey(context), role = roleKey(context), entity = context.getDefinition().getEntityKey();
        Session session = HibernateUtil.getSessionFactory().openSession(); Transaction tx = null;
        try {
            tx = session.beginTransaction();
            SQLQuery q = session.createSQLQuery("insert into generic_crud_user_view(user_key,active_role_key,entity_key,view_name,is_default,visible_columns_json,column_order_json,column_widths_json,pinned_columns_json,page_size,density,sort_json,filter_json,ui_state_json) values(:u,:r,:e,'default',true,:v,:o,:w,:p,:s,:d,:so,:f,:ui) on conflict (user_key,active_role_key,entity_key,view_name) do update set visible_columns_json=excluded.visible_columns_json,column_order_json=excluded.column_order_json,column_widths_json=excluded.column_widths_json,pinned_columns_json=excluded.pinned_columns_json,page_size=excluded.page_size,density=excluded.density,sort_json=excluded.sort_json,filter_json=excluded.filter_json,ui_state_json=excluded.ui_state_json,updated_at=now()");
            q.setString("u", user); q.setString("r", role); q.setString("e", entity);
            q.setString("v", GenericCrudJson.toJson(safe.get("columns"))); q.setString("o", GenericCrudJson.toJson(safe.get("columns")));
            q.setString("w", GenericCrudJson.toJson(safe.get("widths"))); q.setString("p", GenericCrudJson.toJson(safe.get("pinned")));
            q.setInteger("s", ((Number) safe.get("pageSize")).intValue()); q.setString("d", String.valueOf(safe.get("density")));
            q.setString("so", GenericCrudJson.toJson(safe.get("sort"))); q.setString("f", GenericCrudJson.toJson(safe.get("filters"))); q.setString("ui", "{}"); q.executeUpdate(); tx.commit();
        } catch (Exception migrationMissing) {
            rollback(tx); context.getRequest().getSession(true).setAttribute(SESSION_PREFIX + role + "." + entity, safe);
        } finally { close(session); }
        return GenericCrudResult.ok("Preferensi tampilan tersimpan.", safe);
    }

    public GenericCrudResult reset(GenericCrudRequestContext context) throws Exception {
        Map defaults = defaults(context.getDefinition());
        String role = roleKey(context), entity = context.getDefinition().getEntityKey();
        context.getRequest().getSession(true).removeAttribute(SESSION_PREFIX + role + "." + entity);
        Session session = HibernateUtil.getSessionFactory().openSession(); Transaction tx = null;
        try { tx = session.beginTransaction(); SQLQuery q = session.createSQLQuery("delete from generic_crud_user_view where user_key=:u and active_role_key=:r and entity_key=:e and view_name='default'"); q.setString("u", userKey(context)); q.setString("r", role); q.setString("e", entity); q.executeUpdate(); tx.commit(); }
        catch (Exception ignored) { rollback(tx); } finally { close(session); }
        return GenericCrudResult.ok("Preferensi dikembalikan ke default.", defaults);
    }

    public Map sanitize(GenericCrudDefinition definition, Map requested) throws GenericCrudException {
        Map safe = new LinkedHashMap(); Object columns = requested == null ? null : requested.get("columns");
        safe.put("columns", validateColumns(definition, columns instanceof List ? (List) columns : null));
        safe.put("widths", validateNamedMap(definition, requested == null ? null : requested.get("widths")));
        safe.put("pinned", validateColumns(definition, requested != null && requested.get("pinned") instanceof List ? (List) requested.get("pinned") : null));
        int pageSize = requested != null && requested.get("pageSize") instanceof Number ? ((Number) requested.get("pageSize")).intValue() : definition.getDefaultPageSize();
        if (!(pageSize == 5 || pageSize == 10 || pageSize == 25 || pageSize == 50 || pageSize == 100 || pageSize == 500 || pageSize == 1000) || pageSize > definition.getMaxPageSize()) pageSize = definition.getDefaultPageSize();
        safe.put("pageSize", Integer.valueOf(pageSize)); String density = requested == null ? null : String.valueOf(requested.get("density")); safe.put("density", "COMPACT".equals(density) ? "COMPACT" : "COMFORTABLE");
        safe.put("sort", validateSort(definition, requested == null ? null : requested.get("sort")));
        safe.put("filters", validateFilters(definition, requested == null ? null : requested.get("filters"))); return safe;
    }

    public List validateColumns(GenericCrudDefinition definition, List requested) throws GenericCrudException {
        List result = new ArrayList(); if (requested == null) return defaultColumns(definition);
        for (int i = 0; i < requested.size(); i++) { String property = String.valueOf(requested.get(i)); GenericCrudFieldDefinition field = definition.getField(property); if (field == null || field.isSensitive() || !field.isReadable()) continue; if (!result.contains(property)) result.add(property); }
        return result.isEmpty() ? defaultColumns(definition) : result;
    }
    private Map validateNamedMap(GenericCrudDefinition d, Object value) { Map result = new LinkedHashMap(); if (!(value instanceof Map)) return result; java.util.Iterator it = ((Map) value).keySet().iterator(); while (it.hasNext()) { String key = String.valueOf(it.next()); GenericCrudFieldDefinition f = d.getField(key); if (f != null && f.isReadable() && !f.isSensitive()) result.put(key, ((Map) value).get(key)); } return result; }
    private Map defaults(GenericCrudDefinition d) { Map m = new LinkedHashMap(); m.put("columns", defaultColumns(d)); m.put("widths", new LinkedHashMap()); m.put("pinned", new ArrayList()); m.put("pageSize", Integer.valueOf(d.getDefaultPageSize())); m.put("density", "COMFORTABLE"); return m; }
    private List defaultColumns(GenericCrudDefinition d) { List result = new ArrayList(); List fields = d.getFields(); for (int i = 0; i < fields.size(); i++) { GenericCrudFieldDefinition f = (GenericCrudFieldDefinition) fields.get(i); if (f.isTableVisible() && f.isReadable() && !f.isSensitive()) result.add(f.getProperty()); } return result; }
    private Map validateSort(GenericCrudDefinition d, Object value) { if (!(value instanceof Map)) return new LinkedHashMap(); String property = String.valueOf(((Map) value).get("property")); GenericCrudFieldDefinition f = d.getField(property); if (f == null || !f.isReadable() || !f.isSortable() || f.isSensitive()) return new LinkedHashMap(); Map safe = new LinkedHashMap(); safe.put("property", property); safe.put("direction", "DESC".equals(String.valueOf(((Map) value).get("direction"))) ? "DESC" : "ASC"); return safe; }
    private List validateFilters(GenericCrudDefinition d, Object value) { List result = new ArrayList(); if (!(value instanceof List)) return result; List input = (List) value; for (int i = 0; i < input.size() && i < 20; i++) { if (!(input.get(i) instanceof Map)) continue; Map f = (Map) input.get(i); String property = String.valueOf(f.get("property")); GenericCrudFieldDefinition def = d.getField(property); if (def == null || !def.isReadable() || def.isSensitive()) continue; Map safe = new LinkedHashMap(); safe.put("property", property); safe.put("operator", f.get("operator")); safe.put("value", f.get("value")); result.add(safe); } return result; }
    private Map fromRow(GenericCrudDefinition d, Object[] row) throws Exception { Map m = new LinkedHashMap(); m.put("columns", validateColumns(d, (List) GenericCrudJson.fromJson(String.valueOf(row[0]), List.class))); m.put("widths", row[2] == null ? new LinkedHashMap() : GenericCrudJson.fromJson(String.valueOf(row[2]), Map.class)); m.put("pinned", row[3] == null ? new ArrayList() : GenericCrudJson.fromJson(String.valueOf(row[3]), List.class)); m.put("pageSize", row[4]); m.put("density", row[5]); m.put("sort", row[6] == null ? null : GenericCrudJson.fromJson(String.valueOf(row[6]), Map.class)); m.put("filters", row[7] == null ? null : GenericCrudJson.fromJson(String.valueOf(row[7]), List.class)); return sanitize(d, m); }
    static String userKey(GenericCrudRequestContext c) { return c.getUser() == null ? "" : String.valueOf(c.getUser().getUserId()); }
    static String roleKey(GenericCrudRequestContext c) { try { return c.getUser().hakAkses().getRoleId(); } catch (Exception e) { return ""; } }
    private void rollback(Transaction tx) { try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignored) { } }
    private void close(Session s) { try { if (s != null && s.isOpen()) s.close(); } catch (Exception ignored) { } }
}
