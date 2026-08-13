package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudOperation;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;
import ais.action.master.generic.v2.GenericCrudScopeGuard;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Berkas;
import ais.database.model.GeneralValueObject;

/** Parity BerkasAction: CRUD hierarchy, tambah anak, copy node, dan delete leaf-only. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class BerkasGenericCrudAdapter extends AbstractGenericCrudEntityAdapter<Berkas>
        implements GenericCrudScopeAdapter, GenericCrudCustomActionProvider {
    public Berkas createNew(GenericCrudRequestContext context) { return new Berkas(); }

    public void validateCreate(Map values, GenericCrudRequestContext context, List errors) { requiredName(values, errors); }
    public void validateUpdate(Berkas current, Map values, GenericCrudRequestContext context, List errors) { requiredName(values, errors); }
    private void requiredName(Map values, List errors) {
        if (!values.containsKey("nama") || values.get("nama") == null
                || String.valueOf(values.get("nama")).trim().length() == 0) errors.add("nama:Nama Berkas wajib diisi");
    }

    public void beforeSave(Session session, Berkas target, GenericCrudRequestContext context) throws Exception {
        target.setNama(target.getNama() == null ? null : target.getNama().trim());
        if (target.getNama() == null || target.getNama().length() == 0)
            throw new GenericCrudException(400, "BERKAS_NAME_REQUIRED", "Nama Berkas wajib diisi.");
        assertNoCycle(target);
    }

    private void assertNoCycle(Berkas target) throws GenericCrudException {
        Berkas parent = target.getParent(); int depth = 0;
        while (parent != null) {
            if (parent == target || (target.getId() != null && target.getId().equals(parent.getId())))
                throw new GenericCrudException(409, "BERKAS_HIERARCHY_CYCLE", "Induk Berkas tidak boleh menunjuk dirinya atau turunannya.");
            parent = parent.getParent();
            if (++depth > 1000) throw new GenericCrudException(409, "BERKAS_HIERARCHY_CYCLE", "Hierarchy Berkas terdeteksi bersiklus.");
        }
    }

    public boolean canDelete(Berkas target, GenericCrudRequestContext context, List reasons) throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession(); Number children;
        try { children = (Number) session.createCriteria(Berkas.class)
                .add(Restrictions.eq("parent", target)).setProjection(Projections.rowCount()).uniqueResult();
        } finally { try { session.close(); } catch (Exception ignored) { } }
        if (children != null && children.longValue() > 0L) {
            reasons.add("Berkas masih mempunyai child dan tidak dapat dihapus."); return false;
        }
        return true;
    }
    public void delete(Session session, Berkas target, GenericCrudRequestContext context) { session.delete(target); }
    public List getNaturalKeyProperties() { List result = new ArrayList(); result.add("nama"); return result; }
    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void validateObjectScope(GeneralValueObject object, GenericCrudRequestContext context) { }

    public List getActions(GenericCrudDefinition definition, GenericCrudRequestContext context) {
        List result = new ArrayList(); boolean enabled = context != null && context.isCanCreate() && context.isCanUpdate();
        result.add(action("add_child", "Tambah Child", enabled)); result.add(action("copy_node", "Copy Data", enabled));
        return result;
    }
    private Map action(String key, String label, boolean enabled) {
        Map action = new LinkedHashMap(); action.put("actionKey", key); action.put("label", label);
        action.put("requiredPrivilege", GenericCrudOperation.CREATE); action.put("selectionMode", "SINGLE");
        action.put("enabled", Boolean.valueOf(enabled)); action.put("dangerous", Boolean.FALSE);
        List names = new ArrayList(); names.add("newName"); action.put("parameterNames", names);
        Map parameter = new LinkedHashMap(); parameter.put("name", "newName"); parameter.put("label", "Nama Berkas baru");
        parameter.put("required", Boolean.TRUE); List parameters = new ArrayList(); parameters.add(parameter);
        action.put("parameters", parameters); return action;
    }

    public GenericCrudResult execute(String actionKey, List selectedIds, Map parameters,
            GenericCrudRequestContext context) throws Exception {
        if (!"add_child".equals(actionKey) && !"copy_node".equals(actionKey))
            return GenericCrudResult.error("ACTION_NOT_ALLOWED", "Aksi tidak dikenal.");
        if (context == null || !context.isCanUpdate())
            throw new GenericCrudException(403, "UPDATE_REQUIRED", "Hak UPDATE diperlukan untuk mengubah hierarchy Berkas.");
        String newName = parameters == null || parameters.get("newName") == null ? ""
                : String.valueOf(parameters.get("newName")).trim();
        if (newName.length() == 0) throw new GenericCrudException(400, "BERKAS_NAME_REQUIRED", "Nama Berkas baru wajib diisi.");
        Long id; try { id = Long.valueOf(String.valueOf(selectedIds.get(0))); }
        catch (Exception invalid) { throw new GenericCrudException(400, "BERKAS_ID_INVALID", "ID Berkas tidak valid."); }
        Session session = HibernateUtil.currentNativeSession(); Transaction tx = null; Berkas created = null;
        try {
            tx = session.beginTransaction(); Berkas source = (Berkas) session.get(Berkas.class, id);
            if (source == null) throw new GenericCrudException(404, "ROW_NOT_FOUND", "Berkas sumber tidak ditemukan.");
            new GenericCrudScopeGuard().validateObject(source, context);
            created = (Berkas) source.clone(); created.setId(null); created.setNama(newName);
            if ("add_child".equals(actionKey)) created.setParent(source);
            beforeSave(session, created, context); session.save(created); session.flush(); tx.commit();
        } catch (Exception failure) {
            try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignored) { } throw failure;
        } finally { HibernateUtil.closeSession(); }
        CommonPrivilages.saveActivity(getClass(), CommonPrivilages.CREATE, created,
                "add_child".equals(actionKey) ? "Tambah child Berkas New UI" : "Copy Berkas New UI");
        Map data = new LinkedHashMap(); data.put("id", created.getId());
        return GenericCrudResult.ok("Berkas baru berhasil dibuat.", data);
    }
}
