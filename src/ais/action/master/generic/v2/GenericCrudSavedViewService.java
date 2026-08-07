package ais.action.master.generic.v2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.database.hibernate.HibernateUtil;

/** Saved view PRIVATE, selalu terikat owner user + active role + entity. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class GenericCrudSavedViewService {
    private final GenericCrudColumnPreferenceService preferences = new GenericCrudColumnPreferenceService();

    public List list(GenericCrudRequestContext context) throws Exception {
        require(context); Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            SQLQuery q = session.createSQLQuery("select id,view_name,description,is_default,updated_at from generic_crud_saved_view where owner_user_key=:u and owner_role_key=:r and entity_key=:e and active=true order by view_name"); bind(q, context); List rows = q.list(); List result = new ArrayList();
            for (int i = 0; i < rows.size(); i++) { Object[] row = (Object[]) rows.get(i); Map item = new LinkedHashMap(); item.put("id", row[0]); item.put("name", row[1]); item.put("description", row[2]); item.put("default", row[3]); item.put("updatedAt", row[4]); result.add(item); }
            return result;
        } catch (Exception missing) { throw new GenericCrudException(503, "MIGRATION_REQUIRED", "Jalankan migration Generic CRUD 001 sebelum saved view diaktifkan."); }
        finally { close(session); }
    }

    public GenericCrudResult save(GenericCrudRequestContext context, String name, String description, Map requested) throws Exception {
        require(context); if (name == null || name.trim().length() == 0 || name.length() > 250) throw new GenericCrudException(400, "VIEW_NAME_REQUIRED", "Nama saved view wajib diisi maksimal 250 karakter.");
        Map safe = preferences.sanitize(context.getDefinition(), requested); Session session = HibernateUtil.getSessionFactory().openSession(); Transaction tx = null;
        try {
            tx = session.beginTransaction(); SQLQuery q = session.createSQLQuery("insert into generic_crud_saved_view(owner_user_key,owner_role_key,entity_key,view_name,description,visibility,filter_json,sort_json,columns_json,ui_state_json,is_default,active) values(:u,:r,:e,:n,:d,'PRIVATE',:f,:s,:c,'{}',false,true) on conflict (owner_user_key,owner_role_key,entity_key,view_name) do update set description=excluded.description,filter_json=excluded.filter_json,sort_json=excluded.sort_json,columns_json=excluded.columns_json,active=true,updated_at=now()"); bind(q, context); q.setString("n", name.trim()); q.setString("d", description == null ? "" : description); q.setString("f", GenericCrudJson.toJson(safe.get("filters"))); q.setString("s", GenericCrudJson.toJson(safe.get("sort"))); q.setString("c", GenericCrudJson.toJson(safe.get("columns"))); q.executeUpdate(); tx.commit();
            return GenericCrudResult.ok("Saved view tersimpan.", safe);
        } catch (Exception missing) { rollback(tx); throw new GenericCrudException(503, "MIGRATION_REQUIRED", "Jalankan migration Generic CRUD 001 sebelum saved view diaktifkan."); }
        finally { close(session); }
    }

    public GenericCrudResult remove(GenericCrudRequestContext context, Long id) throws Exception {
        require(context); Session session = HibernateUtil.getSessionFactory().openSession(); Transaction tx = null;
        try { tx = session.beginTransaction(); SQLQuery q = session.createSQLQuery("update generic_crud_saved_view set active=false,updated_at=now() where id=:id and owner_user_key=:u and owner_role_key=:r and entity_key=:e"); bind(q, context); q.setLong("id", id.longValue()); int count = q.executeUpdate(); if (count != 1) throw new GenericCrudException(404, "SAVED_VIEW_NOT_FOUND", "Saved view tidak ditemukan untuk user/role aktif."); tx.commit(); return GenericCrudResult.ok("Saved view dihapus.", null); }
        catch (Exception e) { rollback(tx); if (e instanceof GenericCrudException) throw e; throw new GenericCrudException(503, "MIGRATION_REQUIRED", "Tabel saved view belum tersedia."); }
        finally { close(session); }
    }

    private void require(GenericCrudRequestContext c) throws Exception { new GenericCrudPrivilegeGuard().require(c, GenericCrudOperation.READ); if (!c.getDefinition().isSavedViewEnabled()) throw new GenericCrudException(403, "SAVED_VIEW_DISABLED", "Saved view belum diaktifkan."); }
    private void bind(SQLQuery q, GenericCrudRequestContext c) { q.setString("u", GenericCrudColumnPreferenceService.userKey(c)); q.setString("r", GenericCrudColumnPreferenceService.roleKey(c)); q.setString("e", c.getDefinition().getEntityKey()); }
    private void rollback(Transaction tx) { try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignored) { } }
    private void close(Session s) { try { if (s != null && s.isOpen()) s.close(); } catch (Exception ignored) { } }
}
